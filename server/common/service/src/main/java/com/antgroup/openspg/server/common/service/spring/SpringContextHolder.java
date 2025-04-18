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

package com.antgroup.openspg.server.common.service.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextHolder implements ApplicationContextAware {

  private static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    synchronized (this) {
      if (SpringContextHolder.applicationContext == null) {
        SpringContextHolder.applicationContext = applicationContext;
      }
    }
  }

  public static <T> T getBean(Class<T> clazz) {
    if (applicationContext != null) {
      return applicationContext.getBean(clazz);
    }
    return null;
  }

  /** get spring bean by name */
  public static <T> T getBean(String name, Class<T> clazz) {
    if (applicationContext != null) {
      return applicationContext.getBean(name, clazz);
    }
    return null;
  }

  public static Object getBean(String name) {
    if (applicationContext != null) {
      return applicationContext.getBean(name);
    }
    return null;
  }

  public static <T> List<T> getBeans(Class<T> clazz) {
    if (applicationContext != null) {
      return new ArrayList<>(applicationContext.getBeansOfType(clazz).values());
    }
    return null;
  }

  public static <T> Map<String, T> getBeanMap(Class<T> clazz) {
    if (applicationContext != null) {
      return applicationContext.getBeansOfType(clazz);
    }
    return null;
  }

  public static String[] getBeanDefinitionNames() {
    if (applicationContext != null) {
      return applicationContext.getBeanDefinitionNames();
    }
    return null;
  }
}
