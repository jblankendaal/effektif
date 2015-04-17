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

import com.effektif.workflow.api.mapper.BpmnWritable;
import com.effektif.workflow.api.mapper.BpmnWriter;
import com.effektif.workflow.api.mapper.TypeName;


/**
 * Indicates the type of workflow instance variable data.
 *
 * @author Tom Baeyens
 */
public class Type implements BpmnWritable {

  protected String typeName = getClass().getAnnotation(TypeName.class).value();

  /**
   * Default implementation, which just adds a <code>type</code> attribute.
   * Types with additional parameters will need to override and write additional XML.
   */
  @Override
  public void writeBpmn(BpmnWriter w) {
    if (typeName == null) {
      throw new RuntimeException("No @TypeName annotation for class " + getClass().getName());
    }
    w.writeStringAttributeBpmn("type", typeName);
  }
}
