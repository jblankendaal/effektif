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
package com.effektif.workflow.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.effektif.workflow.api.workflow.ParseIssue;
import com.effektif.workflow.api.workflow.WorkflowInstanceMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.model.Deployment;
import com.effektif.workflow.api.model.Message;
import com.effektif.workflow.api.model.TriggerInstance;
import com.effektif.workflow.api.model.TypedValue;
import com.effektif.workflow.api.model.VariableValues;
import com.effektif.workflow.api.model.WorkflowId;
import com.effektif.workflow.api.model.WorkflowInstanceId;
import com.effektif.workflow.api.query.WorkflowInstanceQuery;
import com.effektif.workflow.api.query.WorkflowQuery;
import com.effektif.workflow.api.workflow.ExecutableWorkflow;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.effektif.workflow.impl.configuration.Brewable;
import com.effektif.workflow.impl.configuration.Brewery;
import com.effektif.workflow.impl.data.DataTypeService;
import com.effektif.workflow.impl.util.Exceptions;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflow.TransitionImpl;
import com.effektif.workflow.impl.workflow.WorkflowImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;
import com.effektif.workflow.impl.workflowinstance.LockImpl;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;
import com.effektif.workflow.impl.workflowinstance.WorkflowInstanceImpl;

/**
 * @author Tom Baeyens
 */
public class WorkflowEngineImpl implements WorkflowEngine, Brewable {

  public static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

  public String id;
  public ExecutorService executorService;
  public WorkflowCache workflowCache;
  public WorkflowStore workflowStore;
  public WorkflowInstanceStore workflowInstanceStore;
  public Configuration configuration;
  public List<WorkflowExecutionListener> workflowExecutionListeners;
  public DataTypeService dataTypeService;


  @Override
  public void brew(Brewery brewery) {
    this.id = brewery.get(WorkflowEngineConfiguration.class).getWorkflowEngineId();
    this.configuration = brewery.get(Configuration.class);
    this.executorService = brewery.get(ExecutorService.class);
    this.workflowCache = brewery.get(WorkflowCache.class);
    this.workflowStore = brewery.get(WorkflowStore.class);
    this.workflowInstanceStore = brewery.get(WorkflowInstanceStore.class);
    this.dataTypeService = brewery.get(DataTypeService.class);
  }

  public void startup() {
  }

  public void shutdown() {
    executorService.shutdown();
  }

  /// Workflow methods ////////////////////////////////////////////////////////////

  @Override
  public Deployment deployWorkflow(ExecutableWorkflow workflow) {
    if (log.isDebugEnabled()) {
      log.debug("Deploying workflow");
    }

    WorkflowParser parser = new WorkflowParser(configuration);
    parser.parse(workflow);

    if (!parser.hasErrors()) {
      WorkflowImpl workflowImpl = parser.getWorkflow();
      WorkflowId workflowId;
      if (workflow.getId()==null) {
        workflowId = workflowStore.generateWorkflowId();
        workflow.setId(workflowId);
      }
      workflow.setCreateTime(Time.now());
      workflowImpl.id = workflow.getId();
      workflowStore.insertWorkflow(workflow);
      if (workflowImpl.trigger!=null) {
        workflowImpl.trigger.published(workflowImpl);
      }
      workflowCache.put(workflowImpl);
    }

    return new Deployment(workflow.getId(), parser.getIssues());
  }

  @Override
  public Deployment deployWorkflow(ExecutableWorkflow workflow, WorkflowInstanceMigrator migrator) {

    Deployment deployment = null;

    if (migrator!=null && migrator.originalWorkflowId != null) {
      // create a unique lockOwner for the migration
      UUID uuid = UUID.randomUUID();

      String uniqueLockOwner = getId() + "-" + uuid.toString();

      log.debug("Migration from workflow " + migrator.originalWorkflowId + " started, locking workflowInstances.");

      Long lockResult = lockAllWorkflowInstancesWithRetry(migrator.originalWorkflowId, uniqueLockOwner);

      if (lockResult != null) {
        // Update to new workflow and unlock all
        deployment = deployWorkflow(workflow);

        log.debug("Locking done, now migrating from workflow " + migrator.originalWorkflowId + " to workflow " + deployment.getWorkflowId() + ", and unlocking.");

        workflowInstanceStore.migrateAndUnlockAllLockedWorkflowInstances(migrator.originalWorkflowId, deployment.getWorkflowId().getInternal(), uniqueLockOwner);

        log.debug("Migration of workflowId " + migrator.originalWorkflowId + " to workflowId " + deployment.getWorkflowId() + " finished.");
      } else {
        deployment = new Deployment();
        // Just unlock all
        log.debug("Failed to get a lock on all workflowInstances of workflow " + migrator.originalWorkflowId + ". Unlocking and aborting migration.");

        workflowInstanceStore.migrateAndUnlockAllLockedWorkflowInstances(migrator.originalWorkflowId, null, uniqueLockOwner);

        deployment.addIssue(ParseIssue.IssueType.error, null, null, null, "Migration of workflowInstances of the old workflow failed, because migration failed to get a lock on all workflowInstances of workflow " + migrator.originalWorkflowId, null);
      }
    }

    return deployment;
  }

  @Override
  public List<ExecutableWorkflow> findWorkflows(WorkflowQuery workflowQuery) {
    return workflowStore.findWorkflows(workflowQuery);
  }

  @Override
  public void deleteWorkflows(WorkflowQuery workflowQuery) {
    workflowStore.deleteWorkflows(workflowQuery);
  }

  public WorkflowInstance start(TriggerInstance triggerInstance) {
    WorkflowInstanceImpl workflowInstance = startInitialize(triggerInstance);
    return startExecute(workflowInstance);
  }

  /** first part of starting a new workflow instance: creating the workflow instance and applying the trigger data */
  public WorkflowInstanceImpl startInitialize(TriggerInstance triggerInstance) {
    WorkflowId workflowId = getLatestWorkflowId(triggerInstance);
    WorkflowImpl workflow = getWorkflowImpl(workflowId);

    LockImpl lock = new LockImpl();
    lock.setTime(Time.now());
    lock.setOwner(getId());

    WorkflowInstanceId workflowInstanceId = triggerInstance.getWorkflowInstanceId();
    if (workflowInstanceId==null) {
      workflowInstanceId = workflowInstanceStore.generateWorkflowInstanceId();
    }

    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl(
            configuration,
            workflow,
            workflowInstanceId,
            triggerInstance,
            lock);

    if (log.isDebugEnabled()) log.debug("Created "+workflowInstance);

    if (workflow.trigger!=null) {
      workflow.trigger.applyTriggerData(workflowInstance, triggerInstance);
    } else {
      workflowInstance.setVariableValues(triggerInstance);
    }

    return workflowInstance;
  }

  /** second part of starting a new workflow instance: executing the start actvities */
  public WorkflowInstance startExecute(WorkflowInstanceImpl workflowInstance) {
    WorkflowImpl workflow = workflowInstance.workflow;
    if (log.isDebugEnabled()) log.debug("Starting "+workflowInstance);

    if (workflow.startActivities!=null) {
      for (ActivityImpl startActivityDefinition: workflow.startActivities) {
        if (workflowInstance.startActivityIds == null
                || workflowInstance.startActivityIds.contains(startActivityDefinition.getId())) {
          workflowInstance.execute(startActivityDefinition);
        }
      }
    } else {
      workflowInstance.endAndPropagateToParent();
    }

    workflowInstanceStore.insertWorkflowInstance(workflowInstance);
    workflowInstance.executeWork();

    return workflowInstance.toWorkflowInstance();
  }

  public WorkflowId getLatestWorkflowId(TriggerInstance triggerInstance) {
    WorkflowId workflowId = triggerInstance.getWorkflowId();
    if (workflowId==null) {
      if (triggerInstance.getSourceWorkflowId()!=null) {
        workflowId = workflowStore.findLatestWorkflowIdBySource(triggerInstance.getSourceWorkflowId());
        if (workflowId==null) throw new RuntimeException("No workflow found for source '"+triggerInstance.getSourceWorkflowId()+"'");
      } else {
        throw new RuntimeException("No workflow specified");
      }
    }
    return workflowId;
  }

  @Override
  public WorkflowInstance send(Message message) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(message.getWorkflowInstanceId());
    return send(message, workflowInstance);
  }

  /***
   * To manually move a workflowInstance from the current activityInstance to the specified activityInstance.
   * Any "work" in between will not be executed! Will probably be used during testing of your workflows...
   * Note: If your process contains a parallel gateway, and you move one of the two "instances" to an activity after the
   * merge-parallel gateway, things would get messy.... Because of that, the move does not allow this, it checks for #open activityInstances <= 1
   * Sub-processes are not taken into account ie, propagateToParent is not called.
   * @return WorkflowInstance is the to-activity was found and the move was executed, null otherwise.
   */
  public WorkflowInstance moveImpl(WorkflowInstanceImpl workflowInstanceImpl, String activityInstanceId, String newActivityId) {

    if(workflowInstanceImpl.lock == null) throw new RuntimeException("WorkflowInstance not locked!");

    if (log.isDebugEnabled()) log.debug("Moving workflowInstance to activityId: " + newActivityId);

    try {
      if (workflowInstanceImpl.activityInstances == null) {
        log.debug("ActivityInstances == null, returning without doing something.");
        return null;
      }

      ActivityInstanceImpl activityInstanceImpl = null;
      int openActCount = 0;
      if (activityInstanceId == null) {
        for (ActivityInstanceImpl activityInstance : workflowInstanceImpl.activityInstances) {
          if (!activityInstance.isEnded()) {
            activityInstanceImpl = activityInstance;
            openActCount++;
          }
        }
      } else {
        activityInstanceImpl = workflowInstanceImpl.findActivityInstanceByActivityId(activityInstanceId);
      }

      if (openActCount > 1)
        throw new RuntimeException("Move cannot be called on a workflowInstance with more than one open activityInstance. " +
                "Propably this workflowInstance is part of a paralell process...");

      ActivityImpl activityImpl = workflowInstanceImpl.workflow.findActivityByIdLocal(newActivityId);
      if (activityImpl == null) throw new RuntimeException("To-activityId not found!");

      if (activityInstanceImpl != null && !activityInstanceImpl.isEnded()) activityInstanceImpl.end();
      if (workflowInstanceImpl.isEnded()) {
        workflowInstanceImpl.setEnd(null);
        workflowInstanceImpl.duration = 0L;
      }

      workflowInstanceImpl.execute(activityImpl);
      workflowInstanceImpl.executeWork();

      return workflowInstanceImpl.toWorkflowInstance();
    } finally {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstanceImpl.getId());
    }
  }

  @Override
  public WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String activityInstanceId, String newActivityId) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);

    return moveImpl(workflowInstance, activityInstanceId, newActivityId);
  }

  @Override
  public WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String newActivityId) {
    return move(workflowInstanceId, null, newActivityId);
  }

  public WorkflowInstance send(Message message, WorkflowInstanceImpl workflowInstance) {
    workflowInstance.setVariableValues(message);
    String activityInstanceId = message.getActivityInstanceId();
    ActivityInstanceImpl activityInstance = workflowInstance.findActivityInstance(activityInstanceId);
    if (activityInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(message.getWorkflowInstanceId());
      throw new RuntimeException("Activity instance "+activityInstanceId+" not in workflow instance");
    }
    if (log.isDebugEnabled())
      log.debug("Signalling "+activityInstance);
    ActivityImpl activity = activityInstance.getActivity();
    activity.activityType.message(activityInstance);
    workflowInstance.executeWork();
    return workflowInstance.toWorkflowInstance();
  }

  @Override
  public void deleteWorkflowInstances(WorkflowInstanceQuery query) {
    workflowInstanceStore.deleteWorkflowInstances(query);
  }

  @Override
  public List<WorkflowInstance> findWorkflowInstances(WorkflowInstanceQuery query) {
    List<WorkflowInstanceImpl> workflowInstances = workflowInstanceStore.findWorkflowInstances(query);
    return WorkflowInstanceImpl.toWorkflowInstances(workflowInstances);
  }

  /** retrieves the executable form of the workflow using the workflow cache */
  public WorkflowImpl getWorkflowImpl(WorkflowId workflowId) {
    WorkflowImpl workflowImpl = workflowCache.get(workflowId);
    if (workflowImpl==null) {
      ExecutableWorkflow workflow = workflowStore.loadWorkflowById(workflowId);
      if (workflow != null) {
        WorkflowParser parser = new WorkflowParser(configuration);
        workflowImpl = parser.parse(workflow);
        workflowCache.put(workflowImpl);
      }
    }
    return workflowImpl;
  }

  public Long lockAllWorkflowInstancesWithRetry(final String workflowId, final String uniqueLockOwner) {
    Retry<Long> retry = new Retry<Long>() {
      @Override
      public Long tryOnce() {
        return workflowInstanceStore.lockAllWorkflowInstances(workflowId, uniqueLockOwner);
      }

      @Override
      protected void failedWaitingForRetry() {
        log.debug("Locking all workflowInstances for workflow " + workflowId + " failed.... retrying in " + wait + " millis.");
      }
    };

    return retry.tryManyTimes();
  }

  public WorkflowInstanceImpl lockWorkflowInstanceWithRetry(
          final WorkflowInstanceId workflowInstanceId) {
    Retry<WorkflowInstanceImpl> retry = new Retry<WorkflowInstanceImpl>() {
      @Override
      public WorkflowInstanceImpl tryOnce() {
        return workflowInstanceStore.lockWorkflowInstance(workflowInstanceId);
      }
      @Override
      protected void failedWaitingForRetry() {
        if (log.isDebugEnabled()) {
          log.debug("Locking workflow instance "+workflowInstanceId+" failed... retrying in "+wait+" millis");
        }
      }
      @Override
      protected void interrupted() {
        if (log.isDebugEnabled()) {
          log.debug("Waiting for workflow instance lock was interrupted");
        }
      }
      @Override
      protected void failedPermanent() {
        throw new RuntimeException("Couldn't lock workflow instance " + workflowInstanceId);
      }
    };
    return retry.tryManyTimes();
  }

  public String getId() {
    return id;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public WorkflowCache getProcessDefinitionCache() {
    return workflowCache;
  }

  public WorkflowStore getWorkflowStore() {
    return workflowStore;
  }

  public WorkflowInstanceStore getWorkflowInstanceStore() {
    return workflowInstanceStore;
  }

  public void addWorkflowExecutionListener(WorkflowExecutionListener workflowExecutionListener) {
    if (workflowExecutionListeners==null) {
      workflowExecutionListeners = new ArrayList<>();
    }
    workflowExecutionListeners.add(workflowExecutionListener);
  }

  public void removeWorkflowExecutionListener(WorkflowExecutionListener workflowExecutionListener) {
    if (workflowExecutionListeners!=null) {
      workflowExecutionListeners.remove(workflowExecutionListener);
      if (workflowExecutionListeners.isEmpty()) {
        workflowExecutionListeners = null;
      }
    }
  }

  public List<WorkflowExecutionListener> getWorkflowExecutionListeners() {
    return workflowExecutionListeners;
  }

  public void setWorkflowExecutionListeners(List<WorkflowExecutionListener> workflowExecutionListeners) {
    this.workflowExecutionListeners = workflowExecutionListeners;
  }

  public boolean notifyActivityInstanceStarted(ActivityInstanceImpl activityInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        if (!workflowExecutionListener.starting(activityInstance)) {
          return false;
        }
      }
    }
    return true;
  }

  public void notifyActivityInstanceEnded(ActivityInstanceImpl activityInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.ended(activityInstance);
      }
    }
  }

  public void notifyTransitionTaken(ActivityInstanceImpl activityInstanceFrom, TransitionImpl transition, ActivityInstanceImpl activityInstanceTo) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.transitioning(activityInstanceFrom, transition, activityInstanceTo);
      }
    }
  }

  public VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId) {
    return getVariableValues(workflowInstanceId, null);
  }

  public VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId) {
    WorkflowInstanceImpl workflowInstance = workflowInstanceStore.getWorkflowInstanceImplById(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    VariableValues variableValues = new VariableValues();
    scopeInstance.collectVariableValues(variableValues);
    return variableValues;
  }

  public void setVariableValues(WorkflowInstanceId workflowInstanceId, VariableValues variableValues) {
    setVariableValues(workflowInstanceId, null, variableValues);
  }

  public void setVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId, VariableValues variableValues) {
    if (workflowInstanceId==null || variableValues==null) {
      return;
    }
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    if (scopeInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstanceId);
      throw new RuntimeException("Workflow instance "+workflowInstanceId+" didn't contain active activityInstanceId "+activityInstanceId);
    }
    Map<String, TypedValue> values = variableValues!=null ? variableValues.getValues() : null;
    if (values!=null) {
      for (String variableId : values.keySet()) {
        TypedValue typedValue = values.get(variableId);
        Object value = typedValue.getValue();
        scopeInstance.setVariableValue(variableId, value);
      }
    }
    workflowInstanceStore.flushAndUnlock(workflowInstance);
  }

  public void setVariableValue(WorkflowInstanceId workflowInstanceId, String activityInstanceId, String variableId, Object value) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    if (scopeInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstanceId);
      throw new RuntimeException("Workflow instance "+workflowInstanceId+" didn't contain active activityInstanceId "+activityInstanceId);
    }
    scopeInstance.setVariableValue(variableId, value);
    workflowInstanceStore.flushAndUnlock(workflowInstance);
  }

  protected ScopeInstanceImpl getScopeInstance(WorkflowInstanceImpl workflowInstance, String activityInstanceId) {
    ScopeInstanceImpl scopeInstance = workflowInstance;
    if (activityInstanceId!=null) {
      scopeInstance = workflowInstance.findActivityInstance(activityInstanceId);
      Exceptions.checkNotNull(scopeInstance);
    }
    return scopeInstance;
  }

  public void continueAsync(Runnable asyncContinuation) {

    executorService.execute(asyncContinuation);
  }
}
