package com.effektif.workflow.impl.intermediate;

import com.effektif.workflow.api.json.TypeName;
import com.effektif.workflow.api.workflow.Timer;
import com.effektif.workflow.impl.job.JobController;
import com.effektif.workflow.impl.job.JobType;
import com.effektif.workflow.impl.job.TimerType;
import com.effektif.workflow.impl.workflow.TimerImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;

/**
 * CumulusPro
 * Created by Jeroen on 16/06/16.
 */
@TypeName("IntermediateThrowEventTimer")
public class IntermediateThrowEventTimerImpl implements TimerType, JobType {
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
        ActivityInstanceImpl activityInstance = jobController.getWorkflowInstance().findActivityInstance(jobController.getJob().getActivityInstanceId());

        if (activityInstance != null) activityInstance.onwards();
        jobController.getWorkflowInstance().executeWork();
    }

    @Override
    public Class<? extends Timer> getTimerApiClass() {
        return IntermediateThrowEventTimer.class;
    }

    @Override
    public JobType getJobType(ScopeInstanceImpl scopeInstance, TimerImpl timerImpl) {
        return this;
    }

    @Override
    public boolean isWorkflowTimer() {
        return false;
    }
}
