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
package com.effektif.workflow.api.workflow;


import com.effektif.workflow.api.bpmn.BpmnReader;
import com.effektif.workflow.api.bpmn.XmlElement;

import java.util.List;

/**
 * @author Tom Baeyens
 */
public class Timer {

  protected String id;
  protected String dueDateExpression;
  protected String repeatExpression;
  protected String timeCycleExpression;

  public void readBpmn(BpmnReader r) {
    //            <bpmn:timerEventDefinition>
    //              <bpmn:timeDuration>PT5M</bpmn:timeDuration>
    //            </bpmn:timerEventDefinition>

    // One of three options: MUTUALLY EXCLUSIVE
    //timeDate  -- absolute time, ISO-8601 format
    //timeCycle -- not supported for now
    //timeDuration -- Interval/duration, ISO-8601 format for time interval representations

    this.repeatExpression = r.readTextBpmn("timeDuration"); // ie P2DT5H30M
    this.dueDateExpression = r.readTextBpmn("timeDate");  // ie 2016-04-11T14:13:14
    this.timeCycleExpression = r.readTextBpmn("timeCycle"); // ie: R4/2016-03-11T14:13/PT5M - 4 times every 5 minutes, starting March 11th 14:13
  }

  public String getTimeCycleExpression() {
    return timeCycleExpression;
  }

  public void setTimeCycleExpression(String timeCycleExpression) {
    this.timeCycleExpression = timeCycleExpression;
  }

  public String getId() {
    return this.id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public Timer id(String id) {
    this.id = id;
    return this;
  }

  public String getDueDateExpression() {
    return this.dueDateExpression;
  }
  public void setDueDateExpression(String dueDateExpression) {
    this.dueDateExpression = dueDateExpression;
  }
  public Timer dueDateExpression(String dueDateExpression) {
    this.dueDateExpression = dueDateExpression;
    return this;
  }

  public String getRepeatExpression() {
    return this.repeatExpression;
  }
  public void setRepeatExpression(String repeatExpression) {
    this.repeatExpression = repeatExpression;
  }
  public Timer repeat(String repeatExpression) {
    this.repeatExpression = repeatExpression;
    return this;
  }
}
