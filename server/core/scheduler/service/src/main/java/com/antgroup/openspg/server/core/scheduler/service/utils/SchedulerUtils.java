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
package com.antgroup.openspg.server.core.scheduler.service.utils;

import com.antgroup.openspg.builder.model.record.SubGraphRecord;
import com.antgroup.openspg.common.util.DateTimeUtils;
import com.antgroup.openspg.common.util.StringUtils;
import com.antgroup.openspg.server.core.scheduler.model.service.SchedulerInstance;
import com.antgroup.openspg.server.core.scheduler.model.service.SchedulerTask;
import com.antgroup.openspg.server.core.scheduler.model.task.TaskExecuteDag;
import com.antgroup.openspg.server.core.scheduler.service.metadata.SchedulerTaskService;
import com.google.common.collect.Lists;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.quartz.CronExpression;

/** some scheduler common tools */
@Slf4j
public class SchedulerUtils {

  public static final String EQ = "eq";
  public static final String IN = "in";
  public static final String LT = "lt";

  /** merge two bean by discovering differences */
  public static <M> M merge(M dest, M orig) {
    if (dest == null || orig == null) {
      return (dest == null) ? orig : dest;
    }
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(dest.getClass());
      for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
        if (descriptor.getWriteMethod() == null) {
          continue;
        }
        Object originalValue = descriptor.getReadMethod().invoke(orig);
        if (originalValue == null) {
          continue;
        }
        descriptor.getWriteMethod().invoke(dest, originalValue);
      }
    } catch (Exception e) {
      log.error("merge bean exception", e);
    }
    return dest;
  }

  /** Limit remark. sub String To Length */
  public static String setRemarkLimit(String oldRemark, StringBuffer appendRemark) {
    Integer start = 0;
    Integer length = 100000;
    StringBuffer str = appendRemark.append(oldRemark);
    String fill = "...";
    if (length >= str.length()) {
      return str.toString();
    }
    return str.substring(start, length - fill.length()) + fill;
  }

  /** get CronExpression */
  public static CronExpression getCronExpression(String cron) {
    try {
      return new CronExpression(cron);
    } catch (ParseException e) {
      throw new RuntimeException("Cron ParseException:" + cron, e);
    }
  }

  /** get Cron Execution Dates By Today */
  public static List<Date> getCronExecutionDates(String cron, Date startDate, Date endDate) {
    CronExpression expression = getCronExpression(cron);
    List<Date> dates = new ArrayList<>();

    if (expression.isSatisfiedBy(startDate)) {
      dates.add(startDate);
    }
    Date nextDate = expression.getNextValidTimeAfter(startDate);
    while (nextDate != null && nextDate.before(endDate)) {
      dates.add(nextDate);
      nextDate = expression.getNextValidTimeAfter(nextDate);
    }

    return dates;
  }

  /** get Cron Execution Dates By Today */
  public static List<Date> getCronExecutionDatesByToday(String cron) {
    Date startDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
    Date endDate = DateUtils.addDays(startDate, 1);
    return getCronExecutionDates(cron, startDate, endDate);
  }

  /** get Previous ValidTime */
  public static Date getPreviousValidTime(String cron, Date date) {
    CronExpression expression = getCronExpression(cron);
    Date endDate = expression.getNextValidTimeAfter(expression.getNextValidTimeAfter(date));
    Long time = 2 * date.getTime() - endDate.getTime();

    Date nextDate = expression.getNextValidTimeAfter(new Date(time));
    Date preDate = nextDate;
    while (nextDate != null && nextDate.before(date)) {
      preDate = nextDate;
      nextDate = expression.getNextValidTimeAfter(nextDate);
    }
    return preDate;
  }

  /** get Unique Id */
  public static String getUniqueId(Long jobId, Date schedulerDate) {
    return jobId + DateTimeUtils.getDate2Str(DateTimeUtils.YYYY_MM_DD_HH_MM_SS2, schedulerDate);
  }

  /** content compare key */
  public static boolean compare(Object content, Object key, String type) {
    if (key == null) {
      return true;
    }
    if (content == null) {
      return false;
    }
    switch (type) {
      case EQ:
        return content.equals(key);
      case IN:
        return ((String) content).contains((String) key);
      case LT:
        return ((Date) key).before((Date) content);
      default:
        return false;
    }
  }

  public static void getGraphSize(
      List<SubGraphRecord> records, AtomicLong nodes, AtomicLong edges) {
    nodes = (nodes == null) ? new AtomicLong(0) : nodes;
    edges = (edges == null) ? new AtomicLong(0) : edges;
    for (SubGraphRecord record : records) {
      List<SubGraphRecord.Node> resultNodes = record.getResultNodes();
      if (CollectionUtils.isNotEmpty(resultNodes)) {
        nodes.addAndGet(resultNodes.size());
      }
      List<SubGraphRecord.Edge> resultEdges = record.getResultEdges();
      if (CollectionUtils.isNotEmpty(resultEdges)) {
        edges.addAndGet(resultEdges.size());
      }
    }
  }

  public static List<String> getTaskInputs(
      SchedulerTaskService taskService, SchedulerInstance instance, SchedulerTask task) {
    List<TaskExecuteDag.Node> nodes =
        instance.getTaskDag().getRelatedNodes(task.getNodeId(), false);
    List<String> inputs = Lists.newArrayList();
    nodes.forEach(
        node -> {
          SchedulerTask preTask =
              taskService.queryByInstanceIdAndNodeId(task.getInstanceId(), node.getId());
          if (preTask != null && StringUtils.isNotBlank(preTask.getOutput())) {
            inputs.add(preTask.getOutput());
          }
        });
    return inputs;
  }
}
