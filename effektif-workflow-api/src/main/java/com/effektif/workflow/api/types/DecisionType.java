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
package com.effektif.workflow.api.types;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;


/** Type to represent decision buttons at the end of a form.
 * 
 * @author Tom Baeyens
 */
@JsonTypeName("decision")
public class DecisionType extends TextType {

  protected List<DecisionOption> options;
  
  public List<DecisionOption> getOptions() {
    return options;
  }
  public void setOptions(List<DecisionOption> options) {
    this.options = options;
  }
  public DecisionType option(String option) {
    if (options==null) {
      options = new ArrayList<>();
    }
    options.add(new DecisionOption().name(option));
    return this;
  }
}
