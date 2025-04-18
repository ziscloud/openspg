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

package com.antgroup.openspg.builder.core.physical.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.antgroup.openspg.builder.core.runtime.BuilderContext;
import com.antgroup.openspg.builder.model.exception.PipelineConfigException;
import com.antgroup.openspg.builder.model.record.BaseSPGRecord;
import com.antgroup.openspg.builder.model.record.ChunkRecord;
import com.antgroup.openspg.builder.model.record.RelationRecord;
import com.antgroup.openspg.builder.model.record.SPGRecordTypeEnum;
import com.antgroup.openspg.builder.model.record.SubGraphRecord;
import com.antgroup.openspg.builder.model.record.property.SPGPropertyRecord;
import com.antgroup.openspg.cloudext.interfaces.graphstore.adapter.util.EdgeRecordConvertor;
import com.antgroup.openspg.cloudext.interfaces.graphstore.adapter.util.VertexRecordConvertor;
import com.antgroup.openspg.common.constants.BuilderConstant;
import com.antgroup.openspg.common.util.pemja.PemjaUtils;
import com.antgroup.openspg.common.util.pemja.PythonInvokeMethod;
import com.antgroup.openspg.common.util.pemja.model.PemjaConfig;
import com.antgroup.openspg.core.schema.model.BasicInfo;
import com.antgroup.openspg.core.schema.model.identifier.RelationIdentifier;
import com.antgroup.openspg.core.schema.model.identifier.SPGTypeIdentifier;
import com.antgroup.openspg.core.schema.model.predicate.Property;
import com.antgroup.openspg.core.schema.model.predicate.Relation;
import com.antgroup.openspg.core.schema.model.type.BaseSPGType;
import com.antgroup.openspg.core.schema.model.type.ProjectSchema;
import com.antgroup.openspg.core.schema.model.type.SPGTypeEnum;
import com.antgroup.openspg.core.schema.model.type.SPGTypeRef;
import com.antgroup.openspg.server.api.facade.ApiResponse;
import com.antgroup.openspg.server.api.facade.client.SchemaFacade;
import com.antgroup.openspg.server.api.facade.dto.schema.request.ProjectSchemaRequest;
import com.antgroup.openspg.server.api.http.client.HttpSchemaFacade;
import com.antgroup.openspg.server.api.http.client.util.ConnectionInfo;
import com.antgroup.openspg.server.api.http.client.util.HttpClientBootstrap;
import com.antgroup.openspg.server.common.model.bulider.BuilderJob;
import com.antgroup.openspg.server.common.model.project.Project;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class CommonUtils {

  private static final SPGTypeRef TEXT_REF =
      new SPGTypeRef(new BasicInfo<>(SPGTypeIdentifier.parse("Text")), SPGTypeEnum.BASIC_TYPE);

  public static ProjectSchema getProjectSchema(BuilderContext context) {
    HttpClientBootstrap.init(
        new ConnectionInfo(context.getSchemaUrl()).setConnectTimeout(6000).setReadTimeout(600000));

    SchemaFacade schemaFacade = new HttpSchemaFacade();
    ApiResponse<ProjectSchema> response =
        schemaFacade.queryProjectSchema(new ProjectSchemaRequest(context.getProjectId()));
    if (response.isSuccess()) {
      return response.getData();
    }
    throw new PipelineConfigException(
        "get schema error={}, schemaUrl={}, projectId={}",
        response.getErrorMsg(),
        context.getSchemaUrl(),
        context.getProjectId());
  }

  public static List<BaseSPGRecord> convertNodes(
      SubGraphRecord subGraph, ProjectSchema projectSchema, String namespace) {
    List<SubGraphRecord.Node> resultNodes = subGraph.getResultNodes();
    List<BaseSPGRecord> records = new ArrayList<>();
    if (CollectionUtils.isEmpty(resultNodes)) {
      return records;
    }
    for (SubGraphRecord.Node node : resultNodes) {
      BaseSPGType spgType = projectSchema.getByName(labelPrefix(namespace, node.getLabel()));
      if (spgType == null) {
        continue;
      }
      Map<String, String> stringMap =
          node.getProperties().entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> entry.getValue() == null ? null : entry.getValue().toString()));
      BaseSPGRecord baseSPGRecord =
          VertexRecordConvertor.toAdvancedRecord(spgType, String.valueOf(node.getId()), stringMap);
      records.add(baseSPGRecord);
    }
    records.forEach(CommonUtils::replaceUnSpreadableStandardProperty);
    return records;
  }

  public static String labelPrefix(String namespace, String label) {
    if (label.contains(BuilderConstant.DOT)) {
      return label;
    }
    return namespace + BuilderConstant.DOT + label;
  }

  public static void addLabelPrefix(String namespace, List<SubGraphRecord> records) {
    records.forEach(
        record -> {
          List<SubGraphRecord.Node> resultNodes = record.getResultNodes();
          if (resultNodes != null) {
            resultNodes.forEach(
                resultNode -> {
                  String label = CommonUtils.labelPrefix(namespace, resultNode.getLabel());
                  resultNode.setLabel(label);
                });
          }
          List<SubGraphRecord.Edge> resultEdges = record.getResultEdges();
          if (resultEdges != null) {
            resultEdges.forEach(
                resultEdge -> {
                  String fromType = CommonUtils.labelPrefix(namespace, resultEdge.getFromType());
                  String toType = CommonUtils.labelPrefix(namespace, resultEdge.getToType());
                  resultEdge.setFromType(fromType);
                  resultEdge.setToType(toType);
                });
          }
        });
  }

  public static List<BaseSPGRecord> convertEdges(
      SubGraphRecord subGraph, ProjectSchema projectSchema, String namespace) {
    List<SubGraphRecord.Edge> resultEdges = subGraph.getResultEdges();
    List<BaseSPGRecord> records = new ArrayList<>();
    if (CollectionUtils.isEmpty(resultEdges)) {
      return records;
    }
    for (SubGraphRecord.Edge edge : resultEdges) {
      RelationIdentifier identifier =
          RelationIdentifier.parse(
              labelPrefix(namespace, edge.getFromType())
                  + '_'
                  + edge.getLabel()
                  + '_'
                  + labelPrefix(namespace, edge.getToType()));
      Relation relation = projectSchema.getByName(identifier);
      if (relation == null) {
        continue;
      }
      Map<String, String> stringMap =
          edge.getProperties().entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> entry.getValue() == null ? null : entry.getValue().toString()));
      RelationRecord relationRecord =
          EdgeRecordConvertor.toRelationRecord(
              relation, String.valueOf(edge.getFrom()), String.valueOf(edge.getTo()), stringMap);
      records.add(relationRecord);
    }
    return records;
  }

  private static void replaceUnSpreadableStandardProperty(BaseSPGRecord record) {
    if (SPGRecordTypeEnum.RELATION.equals(record.getRecordType())) {
      return;
    }

    record
        .getProperties()
        .forEach(
            property -> {
              Property propertyType = ((SPGPropertyRecord) property).getProperty();
              propertyType.setObjectTypeRef(TEXT_REF);
              property.getValue().setSingleStd(property.getValue().getRaw());
            });
  }

  public static List<ChunkRecord.Chunk> readSource(
      String pythonExec,
      String pythonPaths,
      String pythonEnv,
      String hostAddr,
      Project project,
      BuilderJob job,
      Date bizDate) {
    PythonInvokeMethod bridgeReader = PythonInvokeMethod.BRIDGE_READER;
    JSONObject pyConfig = new JSONObject();
    JSONObject scanner = new JSONObject();
    JSONObject reader = new JSONObject();
    com.antgroup.openspg.common.util.CommonUtils.getScannerReaderConfig(
        project, job, scanner, reader);
    pyConfig.put(BuilderConstant.SCANNER, scanner);
    pyConfig.put(BuilderConstant.READER, reader);
    String input = com.antgroup.openspg.common.util.CommonUtils.getKagBuilderInput(job, bizDate);
    PemjaConfig config =
        new PemjaConfig(
            pythonExec,
            pythonPaths,
            pythonEnv,
            hostAddr,
            project.getId(),
            bridgeReader,
            Maps.newHashMap());
    List<Object> result = (List<Object>) PemjaUtils.invoke(config, pyConfig.toJSONString(), input);
    List<ChunkRecord.Chunk> chunkList =
        JSON.parseObject(
            JSON.toJSONString(result), new TypeReference<List<ChunkRecord.Chunk>>() {});
    return chunkList;
  }

  public static List<Map<String, Object>> scanSource(
      String pythonExec,
      String pythonPaths,
      String pythonEnv,
      String hostAddr,
      Project project,
      BuilderJob job,
      Date bizDate) {
    PythonInvokeMethod bridgeReader = PythonInvokeMethod.BRIDGE_SCANNER;

    JSONObject pyConfig = new JSONObject();
    JSONObject scanner = new JSONObject();
    JSONObject reader = new JSONObject();
    com.antgroup.openspg.common.util.CommonUtils.getScannerReaderConfig(
        project, job, scanner, reader);
    pyConfig.put(BuilderConstant.SCANNER, scanner);
    String input = com.antgroup.openspg.common.util.CommonUtils.getKagBuilderInput(job, bizDate);

    PemjaConfig config =
        new PemjaConfig(
            pythonExec,
            pythonPaths,
            pythonEnv,
            hostAddr,
            project.getId(),
            bridgeReader,
            Maps.newHashMap());
    List<Object> result = (List<Object>) PemjaUtils.invoke(config, pyConfig.toJSONString(), input);
    List<Map<String, Object>> datas =
        JSON.parseObject(
            JSON.toJSONString(result), new TypeReference<List<Map<String, Object>>>() {});
    return datas;
  }

  public static PemjaConfig getSplitterConfig(
      JSONObject pyConfig,
      String pythonExec,
      String pythonPaths,
      String pythonEnv,
      String hostAddr,
      Project project,
      JSONObject builderExtension) {
    Long projectId = project.getId();
    PythonInvokeMethod splitter = PythonInvokeMethod.BRIDGE_COMPONENT;
    com.antgroup.openspg.common.util.CommonUtils.getSplitterConfig(
        project, builderExtension, pyConfig);
    PemjaConfig pemjaConfig =
        new PemjaConfig(
            pythonExec, pythonPaths, pythonEnv, hostAddr, projectId, splitter, Maps.newHashMap());

    return pemjaConfig;
  }
}
