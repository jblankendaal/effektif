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
package com.effektif.workflow.impl.conditions;

import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;

import com.effektif.imports.CloningContext;
import com.effektif.model.engine.ExecutionContext;
import com.effektif.model.types.VariableFormat;
import com.effektif.workflow.impl.workflow.BindingImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;


/**
 * @author Tom Baeyens
 */
public abstract class Comparator extends ConditionExpression {
  
  protected BindingImpl<?> left;
  protected BindingImpl<?> right;

  public BindingImpl<?> getLeft() {
    return this.left;
  }
  public void setLeft(BindingImpl<?> left) {
    this.left = left;
  }
  public Comparator left(BindingImpl<?> left) {
    this.left = left;
    return this;
  }

  public BindingImpl<?> getRight() {
    return this.right;
  }
  public void setRight(BindingImpl<?> right) {
    this.right = right;
  }
  public Comparator right(BindingImpl<?> right) {
    this.right = right;
    return this;
  }
  

  @Override
  public boolean eval(ActivityInstanceImpl activityInstance) {
    activityInstance.getTypedValue(left);
    
    TypedValue leftTypedValue = left.eval(executionContext);
    TypedValue rightTypedValue = right.eval(executionContext);
    // this is supposed to be a fix for comparisons of typed
    // variable values and plain values, the assumption is
    // that both should have the same type anyway
    if (leftTypedValue.type != null && rightTypedValue.type == null) {
      rightTypedValue.applyType(leftTypedValue.type, VariableFormat.REST_API);
    } else if (leftTypedValue.type == null && rightTypedValue.type != null) {
      leftTypedValue.applyType(rightTypedValue.type, VariableFormat.REST_API);
    }
    return compare(leftTypedValue, rightTypedValue, executionContext);
  }

  public abstract boolean compare(TypedValue leftValue, TypedValue rightValue, ExecutionContext executionContext);
  
  @Override
  public void collectVariableIdsUsed(Set<ObjectId> variableIdsUsed) {
    if (left!=null) {
      left.collectVariableIdsUsed(variableIdsUsed);
    }
    if (right!=null) {
      right.collectVariableIdsUsed(variableIdsUsed);
    }
  }

  public void onClone(CloningContext ctx) {
    if (left!=null) {
      left.onClone(ctx);
    }
    if (right!=null) {
      right.onClone(ctx);
    }
  }
}