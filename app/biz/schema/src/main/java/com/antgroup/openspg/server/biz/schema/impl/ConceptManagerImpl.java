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

import com.antgroup.openspg.common.util.StringUtils;
import com.antgroup.openspg.core.schema.model.SchemaConstants;
import com.antgroup.openspg.core.schema.model.identifier.ConceptIdentifier;
import com.antgroup.openspg.core.schema.model.identifier.PredicateIdentifier;
import com.antgroup.openspg.core.schema.model.identifier.SPGTypeIdentifier;
import com.antgroup.openspg.core.schema.model.semantic.BaseConceptSemantic;
import com.antgroup.openspg.core.schema.model.semantic.DynamicTaxonomySemantic;
import com.antgroup.openspg.core.schema.model.semantic.LogicalRule;
import com.antgroup.openspg.core.schema.model.semantic.RuleStatusEnum;
import com.antgroup.openspg.core.schema.model.semantic.SPGOntologyEnum;
import com.antgroup.openspg.core.schema.model.semantic.TripleSemantic;
import com.antgroup.openspg.core.schema.model.semantic.request.DefineDynamicTaxonomyRequest;
import com.antgroup.openspg.core.schema.model.semantic.request.DefineTripleSemanticRequest;
import com.antgroup.openspg.core.schema.model.semantic.request.RemoveDynamicTaxonomyRequest;
import com.antgroup.openspg.core.schema.model.semantic.request.RemoveTripleSemanticRequest;
import com.antgroup.openspg.core.schema.model.type.Concept;
import com.antgroup.openspg.core.schema.model.type.ConceptList;
import com.antgroup.openspg.server.biz.common.util.AssertUtils;
import com.antgroup.openspg.server.biz.schema.ConceptManager;
import com.antgroup.openspg.server.common.model.UserInfo;
import com.antgroup.openspg.server.core.schema.service.concept.ConceptSemanticService;
import com.antgroup.openspg.server.core.schema.service.semantic.model.TripleSemanticQuery;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConceptManagerImpl implements ConceptManager {

  @Autowired private ConceptSemanticService conceptService;

  @Override
  public void defineDynamicTaxonomy(DefineDynamicTaxonomyRequest request) {
    AssertUtils.assertParamObjectIsNotNull("dsl", request.getDsl());
    LogicalRule logicalRule =
        new LogicalRule(
            null,
            null,
            null,
            null,
            RuleStatusEnum.PROD,
            request.getDsl(),
            new UserInfo(SchemaConstants.DEFAULT_USER_ID, null));
    DynamicTaxonomySemantic conceptSemantic =
        new DynamicTaxonomySemantic(
            SPGTypeIdentifier.parse(request.getConceptTypeName()),
            new ConceptIdentifier(request.getConceptName()),
            logicalRule);
    conceptService.upsertDynamicTaxonomySemantic(conceptSemantic);
  }

  @Override
  public void removeDynamicTaxonomy(RemoveDynamicTaxonomyRequest request) {
    String conceptTypeName = request.getObjectConceptTypeName();
    String concept = request.getObjectConceptName();
    if (conceptTypeName == null && concept == null) {
      throw new IllegalArgumentException("conceptTypeName and conceptName can not be both null");
    }

    SPGTypeIdentifier conceptTypeIdentifier = null;
    if (StringUtils.isNotBlank(conceptTypeName)) {
      conceptTypeIdentifier = SPGTypeIdentifier.parse(conceptTypeName);
    }

    ConceptIdentifier conceptIdentifier = null;
    if (StringUtils.isNotBlank(concept)) {
      conceptIdentifier = new ConceptIdentifier(concept);
    }
    conceptService.deleteDynamicTaxonomySemantic(conceptTypeIdentifier, conceptIdentifier);
  }

  @Override
  public ConceptList getConceptDetail(String conceptTypeName, String conceptName) {
    // get logical causation semantic between concepts
    List<TripleSemantic> conceptRelationSemantics =
        this.getLogicalCausation(conceptTypeName, conceptName);

    // get dynamic taxonomy semantic of concept
    List<DynamicTaxonomySemantic> dynamicTaxonomySemantics =
        this.getDynamicTaxonomy(conceptTypeName, conceptName);

    Map<ConceptIdentifier, List<BaseConceptSemantic>> resultMap = new HashMap<>();
    conceptRelationSemantics.forEach(
        e -> {
          if (!resultMap.containsKey(e.getSubjectIdentifier())) {
            resultMap.put(e.getSubjectIdentifier(), new ArrayList<>());
          }
          resultMap.get(e.getSubjectIdentifier()).add(e);
        });
    dynamicTaxonomySemantics.forEach(
        e -> {
          if (!resultMap.containsKey(e.getConceptIdentifier())) {
            resultMap.put(e.getConceptIdentifier(), new ArrayList<>());
          }
          resultMap.get(e.getConceptIdentifier()).add(e);
        });

    List<Concept> concepts = new ArrayList<>();
    resultMap.forEach((k, v) -> concepts.add(new Concept(k, v)));
    return new ConceptList(concepts);
  }

  @Override
  public List<TripleSemantic> getReasoningConceptsDetail(List<String> conceptTypeNames) {
    TripleSemanticQuery query = new TripleSemanticQuery();
    query.setObjectTypeNames(conceptTypeNames);
    query.setSpgOntologyEnum(SPGOntologyEnum.REASONING_CONCEPT);
    return conceptService.queryTripleSemantic(query);
  }

  @Override
  public void defineLogicalCausation(DefineTripleSemanticRequest request) {
    AssertUtils.assertParamObjectIsNotNull("dsl", request.getDsl());
    LogicalRule logicalRule =
        new LogicalRule(
            null,
            null,
            null,
            null,
            RuleStatusEnum.PROD,
            request.getDsl(),
            new UserInfo(SchemaConstants.DEFAULT_USER_ID, null));
    TripleSemantic conceptSemantic =
        new TripleSemantic(
            SPGTypeIdentifier.parse(request.getSubjectConceptTypeName()),
            new ConceptIdentifier(request.getSubjectConceptName()),
            new PredicateIdentifier(request.getPredicateName()),
            SPGTypeIdentifier.parse(request.getObjectConceptTypeName()),
            new ConceptIdentifier(request.getObjectConceptName()),
            logicalRule,
            StringUtils.isNotBlank(request.getSemanticType())
                ? SPGOntologyEnum.valueOf(request.getSemanticType())
                : null);
    if (conceptSemantic.getOntologyType() != conceptSemantic.getSemanticType()) {
      conceptSemantic.setOntologyType(conceptSemantic.getSemanticType());
    }
    conceptService.upsertTripleSemantic(conceptSemantic);
  }

  @Override
  public void removeLogicalCausation(RemoveTripleSemanticRequest request) {
    SPGTypeIdentifier subjectType =
        request.getSubjectConceptTypeName() == null
            ? null
            : SPGTypeIdentifier.parse(request.getSubjectConceptTypeName());
    ConceptIdentifier subjectName =
        request.getSubjectConceptName() == null
            ? null
            : new ConceptIdentifier(request.getSubjectConceptName());
    PredicateIdentifier predicateIdentifier = new PredicateIdentifier(request.getPredicateName());
    SPGTypeIdentifier objectType =
        request.getObjectConceptTypeName() == null
            ? null
            : SPGTypeIdentifier.parse(request.getObjectConceptTypeName());
    ConceptIdentifier objectName =
        request.getObjectConceptName() == null
            ? null
            : new ConceptIdentifier(request.getObjectConceptName());

    TripleSemantic conceptSemantic =
        new TripleSemantic(
            subjectType,
            subjectName,
            predicateIdentifier,
            objectType,
            objectName,
            null,
            StringUtils.isNotBlank(request.getSemanticType())
                ? SPGOntologyEnum.valueOf(request.getSemanticType())
                : null);
    conceptService.deleteTripleSemantic(conceptSemantic);
  }

  private List<TripleSemantic> getLogicalCausation(
      String subjectConceptTypeName, String subjectConceptName) {
    TripleSemanticQuery query = new TripleSemanticQuery();
    query.setSubjectTypeNames(Lists.newArrayList(subjectConceptTypeName));
    query.setSubjectName(subjectConceptName);
    return conceptService.queryTripleSemantic(query);
  }

  private List<DynamicTaxonomySemantic> getDynamicTaxonomy(
      String conceptTypeName, String conceptName) {
    SPGTypeIdentifier conceptTypeIdentifier = null;
    if (StringUtils.isNotBlank(conceptTypeName)) {
      conceptTypeIdentifier = SPGTypeIdentifier.parse(conceptTypeName);
    }

    ConceptIdentifier conceptIdentifier = null;
    if (StringUtils.isNotBlank(conceptName)) {
      conceptIdentifier = new ConceptIdentifier(conceptName);
    }
    return conceptService.queryDynamicTaxonomySemantic(conceptTypeIdentifier, conceptIdentifier);
  }
}
