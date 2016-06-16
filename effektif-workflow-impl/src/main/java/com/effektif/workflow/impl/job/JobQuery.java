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
package com.effektif.workflow.impl.job;


import com.effektif.workflow.api.model.WorkflowId;

/**
 * @author Tom Baeyens
 */
public class JobQuery {

  protected String jobId;
  protected WorkflowId workflowId;

  public String getJobId() {
    return this.jobId;
  }
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }
  public JobQuery jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  public WorkflowId getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(WorkflowId workflowId) {
    this.workflowId = workflowId;
  }

  public JobQuery workflowId(WorkflowId workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  public boolean meetsCriteria(Job job) {
    if (jobId!=null && !jobId.equals(job.id) || workflowId != null && !workflowId.equals(job.workflowId)) {
      return false;
    }
    return true;
  }
}
