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
package com.effektif.workflow.api.activities;

import com.effektif.workflow.api.bpmn.BpmnElement;
import com.effektif.workflow.api.bpmn.BpmnReader;
import com.effektif.workflow.api.bpmn.XmlElement;
import com.effektif.workflow.api.condition.Condition;
import com.effektif.workflow.api.json.TypeName;
import com.effektif.workflow.api.model.WorkflowId;
import com.effektif.workflow.api.workflow.Activity;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.api.workflow.Transition;
import com.effektif.workflow.api.workflow.starteventtimer.StartEventTimer;

import java.util.List;


/**
 * A start event activates its outgoing flow. A process may have zero or more start events.
 *
 * @see <a href="https://github.com/effektif/effektif/wiki/Start-Event">Start Event</a>
 * @author Tom Baeyens
 */
@TypeName("startEvent")
@BpmnElement("startEvent")
public class StartEvent extends Activity {

  @Override
  public void readBpmn(BpmnReader r) {

    List <XmlElement> elements = r.readElementsBpmn("timerEventDefinition");
    for (XmlElement element : elements) {
      r.startElement(element);

      Timer timer = new StartEventTimer();
      timer.readBpmn(r);

      this.timer(timer);

      r.endElement();
    }
    super.readBpmn(r);

  }

  @Override
  public StartEvent id(String id) {
    super.id(id);
    return this;
  }
  
  @Override
  public StartEvent name(String name) {
    super.name(name);
    return this;
  }

  @Override
  public StartEvent description(String description) {
    super.description(description);
    return this;
  }

  @Override
  public StartEvent transitionTo(String toActivityId) {
    super.transitionTo(toActivityId);
    return this;
  }

  @Override
  public StartEvent transitionWithConditionTo(Condition condition, String toActivityId) {
    super.transitionWithConditionTo(condition, toActivityId);
    return this;
  }

  @Override
  public StartEvent transitionToNext() {
    super.transitionToNext();
    return this;
  }

  @Override
  public StartEvent transitionTo(Transition transition) {
    super.transitionTo(transition);
    return this;
  }

  @Override
  public StartEvent transition(Transition transition) {
    super.transition(transition);
    return this;
  }

  @Override
  public StartEvent transition(String id, Transition transition) {
    super.transition(id, transition);
    return this;
  }

  @Override
  public StartEvent property(String key, Object value) {
    super.property(key, value);
    return this;
  }

  @Override
  public StartEvent propertyOpt(String key, Object value) {
    super.propertyOpt(key, value);
    return this;
  }
}
