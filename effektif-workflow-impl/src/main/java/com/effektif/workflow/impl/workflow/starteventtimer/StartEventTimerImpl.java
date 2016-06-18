package com.effektif.workflow.impl.workflow.starteventtimer;

import com.effektif.workflow.api.json.TypeName;
import com.effektif.workflow.api.model.TriggerInstance;
import com.effektif.workflow.api.query.WorkflowQuery;
import com.effektif.workflow.api.workflow.Activity;
import com.effektif.workflow.api.workflow.ExecutableWorkflow;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.api.workflow.Trigger;
import com.effektif.workflow.api.workflow.starteventtimer.StartEventTimer;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.effektif.workflow.impl.WorkflowEngineImpl;
import com.effektif.workflow.impl.job.*;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflow.TimerImpl;
import com.effektif.workflow.impl.workflow.WorkflowImpl;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;

import java.util.List;

/**
 * CumulusPro
 * Created by Jeroen on 14/06/16.
 */
@TypeName("startEventTimer")
public class StartEventTimerImpl extends Timer implements TimerType, JobType  {

    @Override
    public Class<? extends Timer> getTimerApiClass() {
        return StartEventTimer.class;
    }

    @Override
    public JobType getJobType(ScopeInstanceImpl scopeInstance, TimerImpl timerImpl) {
        return this;
    }

    @Override
    public boolean isWorkflowTimer() {
        return true;
    }

    @Override
    public int getMaxRetries() {
        return 0;
    }

    @Override
    public int getRetryDelayInSeconds(long retry) {
        return 0;
    }

    @Override
    public void execute(JobController jobController) {
        Job job = jobController.getJob();
        WorkflowEngineImpl engine = (WorkflowEngineImpl) jobController.getConfiguration().getWorkflowEngine();

        // Trigger start activity
        TriggerInstance trigger = new TriggerInstance();
        trigger.workflowId(job.workflowId)
                .addStartActivityId(job.activityId);

        WorkflowInstance workflowInstance = engine.start(trigger);

        // calculate new dueDate
        WorkflowImpl workflow = engine.getWorkflowImpl(job.workflowId);

        TimerImpl timerImpl = null;

        for (ActivityImpl activity: workflow.getActivities().values()) {
            if (job.activityId.equals(activity.getId())) {
                if (activity.getTimers() != null && activity.getTimers().size() > 0) {
                    timerImpl = activity.getTimers().get(0);
                    break;
                }
            }
        }

        if (timerImpl != null) {
            job.rescheduleFor(timerImpl.calculateDueDate());
        }

    }
}
