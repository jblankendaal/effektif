/* Copyright (c) 2014, Effektif GmbH.
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
 * limitations under the License. */
package com.effektif.workflow.impl.mapper;

import com.effektif.workflow.impl.configuration.Brewable;
import com.effektif.workflow.impl.configuration.Brewery;


/**
 * A facade for API object serialisation and deserialisation.
 *
 * TODO Rename to Mapper (‘Abstract’ is redundant).
 * 
 * @author Tom Baeyens
 */
public abstract class AbstractMapper implements Brewable {
  
  protected Mappings mappings;

  @Override
  public void brew(Brewery brewery) {
    this.mappings = brewery.get(Mappings.class);
  }

  public Mappings getMappings() {
    return mappings;
  }

  public void setMappings(Mappings mappings) {
    this.mappings = mappings;
  }
}
