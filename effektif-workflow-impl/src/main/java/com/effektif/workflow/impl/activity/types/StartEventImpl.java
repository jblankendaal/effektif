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
package com.effektif.workflow.impl.activity.types;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.effektif.workflow.api.activities.StartEvent;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.api.workflow.starteventtimer.StartEventTimer;
import com.effektif.workflow.impl.WorkflowParser;
import com.effektif.workflow.impl.activity.AbstractActivityType;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;


/**
 * @author Tom Baeyens
 */
public class StartEventImpl extends AbstractActivityType<StartEvent> {

  public StartEventImpl() {
    super(StartEvent.class);
  }

  @Override
  public void parse(ActivityImpl activityImpl, StartEvent activity, WorkflowParser parser) {
    super.parse(activityImpl, activity, parser);

    if (activity.getTimers() != null) {
      for (Timer timer : activity.getTimers()) {
        if (timer instanceof StartEventTimer) {
          if (timer.getTimeCycleExpression() == null) {
            parser.addError("timeCycle not specified.");
          } else {
            CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
            try {
              Cron cron = cronParser.parse(timer.getTimeCycleExpression());
              cron.validate();
            } catch (IllegalArgumentException ex) {
              parser.addError("Error in timerEventDefinition: " + ex.getMessage());
            }
          }
        }
      }
    }
  }

  @Override
  public void execute(ActivityInstanceImpl activityInstance) {
    activityInstance.onwards();
  }
  
  @Override
  public boolean isFlushSkippable() {
    return true;
  }

}
