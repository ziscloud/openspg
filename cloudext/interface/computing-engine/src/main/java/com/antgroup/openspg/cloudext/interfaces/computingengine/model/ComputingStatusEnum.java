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
package com.antgroup.openspg.cloudext.interfaces.computingengine.model;

public enum ComputingStatusEnum {
  SUBMIT,
  RUNNING,
  SUCCESS,
  FAILED,
  STOP,
  NOTFOUND,
  UNDEFINED;

  public static boolean isException(ComputingStatusEnum status) {
    return ComputingStatusEnum.FAILED.equals(status)
        || ComputingStatusEnum.STOP.equals(status)
        || ComputingStatusEnum.NOTFOUND.equals(status)
        || ComputingStatusEnum.UNDEFINED.equals(status);
  }
}
