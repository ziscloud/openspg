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
package com.antgroup.openspg.server.core.reasoner.service;

import com.antgroup.openspg.core.schema.model.type.ProjectSchema;
import com.antgroup.openspg.reasoner.lube.catalog.Catalog;

public interface CatalogService {

  /**
   * get project schema info
   *
   * @param projectId
   * @param graphStoreUrl
   * @return
   */
  ProjectSchema getSchemaInfo(Long projectId, String graphStoreUrl);

  /**
   * get catalog object
   *
   * @param projectId
   * @param graphStoreUrl
   */
  Catalog getCatalog(Long projectId, String graphStoreUrl);
}
