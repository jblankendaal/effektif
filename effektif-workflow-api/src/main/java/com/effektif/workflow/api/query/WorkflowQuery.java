/* Copyright 2014 Effektif GmbH.
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
package com.effektif.workflow.api.query;

import java.util.ArrayList;
import java.util.List;


public class WorkflowQuery {
  
  public static final String FIELD_DEPLOY_TIME = "deployTime";
  
  protected String workflowId;
  protected String workflowName;
  protected Integer skip;
  protected Integer limit;
  protected List<OrderBy> orderBy;

  public String getWorkflowId() {
    return this.workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
  public WorkflowQuery workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }
  
  public String getWorkflowName() {
    return this.workflowName;
  }
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
  public WorkflowQuery workflowName(String workflowName) {
    this.workflowName = workflowName;
    return this;
  }

  public Integer getSkip() {
    return this.skip;
  }
  public void setSkip(Integer skip) {
    this.skip = skip;
  }
  public WorkflowQuery skip(Integer skip) {
    this.skip = skip;
    return this;
  }
  
  public Integer getLimit() {
    return this.limit;
  }
  public void setLimit(Integer limit) {
    this.limit = limit;
  }
  public WorkflowQuery limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public List<OrderBy> getOrderBy() {
    return orderBy;
  }
  
  public void setOrderBy(List<OrderBy> orderBy) {
    this.orderBy = orderBy;
  }
  
  public WorkflowQuery orderByDeployTime(OrderDirection direction) {
    orderBy(FIELD_DEPLOY_TIME, direction);
    return this;
  }
  
  public void orderBy(String field, OrderDirection direction) {
    if (orderBy==null) {
      orderBy = new ArrayList<>();
    }
    orderBy.add(new OrderBy()
      .field(field)
      .direction(direction));
  }
}
