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

import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.impl.WorkflowParser;
import com.effektif.workflow.impl.job.Job;
import com.effektif.workflow.impl.job.TimerType;
import com.effektif.workflow.impl.job.TimerTypeService;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Tom Baeyens
 */
public class TimerImpl {

  public static final Logger log = LoggerFactory.getLogger(TimerImpl.class);

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

    // Check that only one of the expressions are not null
    int expressionCount = 0;
    if (timer.getTimeDate() != null) expressionCount++;
    if (timer.getTimeCycleExpression() != null) expressionCount++;
    if (timer.getTimeDuration() != null) expressionCount++;

    if (expressionCount > 1) {
      parser.addError("TimeDuration, TimeCycle and TimeDate are mutually exclusive, but more than one is set. Please remove one of them.");
    } else if (expressionCount == 0) {
      parser.addError("timeDuration, timeCycle or timeDate should be set on a timerEventDefinition. None is set currently.");
    }

    try {
      calculateDueDate();
    } catch (RuntimeException ex) {
      parser.addError("Error in timer expression, message: " + ex.getMessage());
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

  public Job createWorkflowJob(ActivityImpl startActivity) {
    if (timerType.isWorkflowTimer()) {
      LocalDateTime dueDate = calculateDueDate();
      if (dueDate != null) {
        Job job = new Job();
        job.key = UUID.randomUUID().toString();
        job.workflowId = startActivity.workflow.id;
        job.activityId = startActivity.id;
        job.dueDate = calculateDueDate();
        job.jobType = timerType.getJobType(null, this);
        return job;
      } else {
        log.warn("Not setting a workflow timer because the calculated duedate is null.");
        return null;
      }
    } else {
      throw new RuntimeException("Attached timer is not a workflow timer.");
    }
  }

  public LocalDateTime calculateDueDate() {

//    String[] parts2 = "R4/2016-03-11T14:13/PT5M".split("[/]");

    if (timer.getTimeDuration() != null) {
      try {
        int seconds = (int) java.time.Duration.parse(timer.getTimeDuration()).getSeconds();

        return Time.now().plusSeconds(seconds);
      } catch (Exception ex) {
        throw new RuntimeException("Expression: " + timer.getTimeDuration(), ex);
      }
    } else if (timer.getTimeDate() != null) {
      try {
        return ISODateTimeFormat.dateTime().parseDateTime(timer.getTimeDate()).toLocalDateTime();
      } catch (Exception ex) {
        throw new RuntimeException("Error parsing duedate: " + timer.getTimeDate(), ex);
      }
    } else if (timer.getTimeCycleExpression() != null) {

      /* http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html */
      try {
        CronExpression cronExpression = new CronExpression(timer.getTimeCycleExpression());
        return new LocalDateTime(cronExpression.getTimeAfter(Time.now().toDate()));
        //return cronExpression.getTimeAfter(Time.now().toDateTime()).toLocalDateTime();
      } catch (Exception ex) {
        throw new RuntimeException("Error parsing cronexpression: " + timer.getTimeCycleExpression(), ex);
      }
    }

    return null;
  }
}

