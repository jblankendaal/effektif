/*
 * Copyright 2014 Effektif GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.effektif.workflow.impl.workflow;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.activities.StartEvent;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.impl.WorkflowParser;
import com.effektif.workflow.impl.job.Job;
import com.effektif.workflow.impl.job.TimerType;
import com.effektif.workflow.impl.job.TimerTypeService;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Tom Baeyens
 */
public class TimerImpl {

  public String id;
  public ScopeImpl parent;
  public Configuration configuration;
  public WorkflowImpl workflow;
  public Timer timer;
  public TimerType timerType;

  public void parse(Timer timer, ScopeImpl parentImpl, WorkflowParser parser) {
    this.configuration = parser.configuration;
    this.timer = timer;
    this.id = timer.getId();
    if (parentImpl!=null) {
      this.parent = parentImpl;
      this.workflow = parentImpl.workflow;
    }

    if (timer.getRepeatExpression() != null && timer.getDueDateExpression() != null) {
      parser.addError("TimeDuration and TimeDate on TimerEventDefinition are both set, but mutually exclusive, please remove one of them.");
    }

    TimerTypeService timerTypeService = parser.getConfiguration(TimerTypeService.class);
    this.timerType = timerTypeService.instantiateTimerType(timer);
    // some activity types need to validate incoming and outgoing transitions, 
    // that's why they are NOT parsed here, but after the transitions.
    if (this.timerType==null) {
      parser.addError("Activity '%s' has no activityType configured", id);
    }
  }

  // TODO add a section in ScopeInstanceImpl.toScopeInstance that 
  // uses this method when serializing timers in 
  public Timer toTimer() {
    Timer timer = new Timer();
    // TODO serialize this into a timer
    return timer;
  }

  public Job createJob(ScopeInstanceImpl scopeInstance) {
    Job job = new Job();
    job.workflowId = scopeInstance.workflow.id;
    job.workflowInstanceId = scopeInstance.workflowInstance.id;
    job.dueDate = calculateDueDate();
    job.jobType = timerType.getJobType(scopeInstance, this);
    return job;
  }

  public Job createJob(ActivityImpl startActivity) {

    if (startActivity.activity instanceof StartEvent) {
      Job job = new Job();
      job.workflowId = startActivity.workflow.id;
      job.activityId = startActivity.id;
      job.dueDate = calculateDueDate();
      job.jobType = timerType.getJobType(null, this);
      return job;
    } else {
      throw new RuntimeException("Job can only be created from a StartEvent type activity.");
    }
  }

  public LocalDateTime calculateDueDate() {

//    String[] parts2 = "R4/2016-03-11T14:13/PT5M".split("[/]");

    if (timer.getRepeatExpression() != null) {
      try {

        int seconds = (int) java.time.Duration.parse(timer.getRepeatExpression()).getSeconds();

        return Time.now().plusSeconds(seconds);
      } catch (Exception ex) {
        throw new RuntimeException("Expression: " + timer.getRepeatExpression(), ex);
      }
    } else if (timer.getDueDateExpression() != null) {
      try {
        return ISODateTimeFormat.dateTime().parseDateTime(timer.getDueDateExpression()).toLocalDateTime();
      } catch (Exception ex) {
        throw new RuntimeException("Error parsing duedate: " + timer.getDueDateExpression(), ex);
      }
    } else if (timer.getTimeCycleExpression() != null) {
      CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
      try {
        Cron cron = cronParser.parse(timer.getTimeCycleExpression());
        cron.validate();
        ExecutionTime executionTime = ExecutionTime.forCron(cronParser.parse(timer.getTimeCycleExpression()));

        return executionTime.nextExecution(Time.now().toDateTime()).toLocalDateTime();
      } catch (IllegalArgumentException ex) {
        throw new RuntimeException("Error parsing cronexpression: " + timer.getTimeCycleExpression(), ex);
      }
    }

    return null;
  }

}

