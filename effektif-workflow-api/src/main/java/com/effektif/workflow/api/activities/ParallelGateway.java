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

import com.effektif.workflow.api.workflow.Activity;
import com.fasterxml.jackson.annotation.JsonTypeName;


/**
 * A parallel gateway, used to fork and join sequence flows for executing tasks in parallel.
 *
 * BPMN XML: {@code <parallelGateway id="fork"/>}
 *
 * @author Tom Baeyens
 */
@JsonTypeName("parallelGateway")
public class ParallelGateway extends Activity {

  public ParallelGateway() {
  }

  public ParallelGateway(String id) {
    super(id);
  }
}
