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
package com.effektif.example.cli.command;

import java.io.PrintWriter;
import java.util.List;

import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.model.Message;
import com.effektif.workflow.api.model.WorkflowInstanceId;
import com.effektif.workflow.api.query.WorkflowInstanceQuery;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;

/**
 * Completes the task with the given ID, by signalling the activity instance with a message.
 */
public class CompleteCommand implements CommandImpl {

  @Override
  public void execute(CommandLine command, Configuration configuration, PrintWriter out) {
    final TaskId taskId = new TaskId(command.getArgument());

    final Message message = new Message()
      .workflowInstanceId(taskId.getWorkflowInstanceId())
      .activityInstanceId(taskId.getActivityInstanceId());

    WorkflowEngine workflowEngine = configuration.getWorkflowEngine();
    workflowEngine.send(message);
  }
}
