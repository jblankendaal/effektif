package com.effektif.workflow.impl.intermediate;

import com.effektif.workflow.impl.WorkflowParser;
import com.effektif.workflow.impl.activity.AbstractActivityType;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;

/**
 * CumulusPro
 * Created by Jeroen on 16/06/16.
 */

public class IntermediateThrowEventImpl extends AbstractActivityType<IntermediateThrowEvent> {
    public IntermediateThrowEventImpl() {
        super(IntermediateThrowEvent.class);
    }

    @Override
    public void parse(ActivityImpl activityImpl, IntermediateThrowEvent activity, WorkflowParser parser) {
        super.parse(activityImpl, activity, parser);
        // todo: check the timer
    }

    @Override
    public void execute(ActivityInstanceImpl activityInstance) {
        // Do nothing here, the timer will take care of advancing the workflowInstance to the next activity.
    }

    @Override
    public boolean isFlushSkippable() {
        return false;
    }
}
