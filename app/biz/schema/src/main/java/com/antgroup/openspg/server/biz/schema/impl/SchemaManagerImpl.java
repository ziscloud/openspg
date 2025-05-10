/*
 * Copyright 2023 OpenSPG Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.antgroup.openspg.server.biz.schema.impl;

import com.antgroup.openspg.core.schema.model.SchemaException;
import com.antgroup.openspg.core.schema.model.identifier.SPGTypeIdentifier;
import com.antgroup.openspg.core.schema.model.predicate.Property;
import com.antgroup.openspg.core.schema.model.predicate.Relation;
import com.antgroup.openspg.core.schema.model.semantic.SPGOntologyEnum;
import com.antgroup.openspg.core.schema.model.type.BaseSPGType;
import com.antgroup.openspg.core.schema.model.type.ProjectSchema;
import com.antgroup.openspg.core.schema.model.type.SPGTypeEnum;
import com.antgroup.openspg.core.schema.model.type.SPGTypeRef;
import com.antgroup.openspg.server.api.facade.dto.schema.request.SchemaAlterRequest;
import com.antgroup.openspg.server.biz.schema.SchemaManager;
import com.antgroup.openspg.server.common.model.exception.LockException;
import com.antgroup.openspg.server.common.model.exception.ProjectException;
import com.antgroup.openspg.server.common.model.project.Project;
import com.antgroup.openspg.server.common.service.lock.DistributeLockService;
import com.antgroup.openspg.server.common.service.project.ProjectService;
import com.antgroup.openspg.server.core.schema.service.alter.SchemaAlterPipeline;
import com.antgroup.openspg.server.core.schema.service.alter.model.SchemaAlterContext;
import com.antgroup.openspg.server.core.schema.service.predicate.model.SimpleProperty;
import com.antgroup.openspg.server.core.schema.service.type.SPGTypeService;
import com.antgroup.openspg.server.core.schema.service.type.model.BuiltInPropertyEnum;
import com.antgroup.openspg.server.core.schema.service.util.PropertyUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class SchemaManagerImpl implements SchemaManager {

  private static final String SCHEMA_DEPLOY_LOCK_KEY = "schema_deploy_lock_";
  private static final Long SCHEMA_DEPLOY_LOCK_TIMEOUT = 5 * 60 * 1000L;

  @Autowired private SPGTypeService spgTypeService;
  @Autowired private ProjectService projectService;
  @Autowired private SchemaAlterPipeline schemaAlterPipeline;
  @Autowired private DistributeLockService distributeLockService;

  @Override
  @Transactional(value = "transactionManager", rollbackFor = Exception.class)
  public void alterSchema(SchemaAlterRequest request) {
    Long projectId = request.getProjectId();
    Project project = projectService.queryById(projectId);
    if (null == project) {
      throw ProjectException.projectNotExist(projectId);
    }

    String lockKey = String.format("%s%s", SCHEMA_DEPLOY_LOCK_KEY, projectId.toString());
    boolean locked = distributeLockService.tryLock(lockKey, SCHEMA_DEPLOY_LOCK_TIMEOUT);
    if (!locked) {
      throw LockException.lockFail(lockKey);
    }

    try {
      ProjectSchema projectSchema = spgTypeService.queryProjectSchema(projectId);
      SchemaAlterContext context =
          new SchemaAlterContext()
              .setProject(project)
              .setReleasedSchema(projectSchema.getSpgTypes())
              .setAlterSchema(request.getSchemaDraft().getAlterSpgTypes());
      schemaAlterPipeline.run(context);
    } catch (Exception e) {
      throw SchemaException.alterError(e);
    } finally {
      distributeLockService.unlock(lockKey);
    }
  }

  @Override
  public ProjectSchema getProjectSchema(Long projectId) {
    Project project = projectService.queryById(projectId);
    if (null == project) {
      throw ProjectException.projectNotExist(projectId);
    }

    return spgTypeService.queryProjectSchema(projectId);
  }

  @Override
  public BaseSPGType getSpgType(String uniqueName) {
    SPGTypeIdentifier spgTypeIdentifier = SPGTypeIdentifier.parse(uniqueName);
    return spgTypeService.querySPGTypeByIdentifier(spgTypeIdentifier);
  }

  @Override
  public List<BaseSPGType> querySPGTypeById(List<Long> uniqueIds) {
    return spgTypeService.querySPGTypeById(uniqueIds);
  }

  @Override
  public List<Relation> queryRelationByUniqueId(List<Long> uniqueIds) {
    return spgTypeService.queryRelationByUniqueId(uniqueIds);
  }

  @Override
  public List<SimpleProperty> queryPropertyByUniqueId(
      List<Long> uniqueIds, SPGOntologyEnum ontologyEnum) {
    return spgTypeService.queryPropertyByUniqueId(uniqueIds, ontologyEnum);
  }

  @Override
  public List<Property> getBuiltInProperty(SPGTypeEnum spgTypeEnum) {
    List<Property> builtInProperties = new ArrayList<>();
    SPGTypeRef spgTypeRef = new SPGTypeRef(null, spgTypeEnum);

    List<BuiltInPropertyEnum> builtInPropertyEnums =
        BuiltInPropertyEnum.getBuiltInProperty(spgTypeEnum);
    builtInPropertyEnums.forEach(
        e -> builtInProperties.add(PropertyUtils.newProperty(spgTypeRef, e)));
    return builtInProperties;
  }
}
