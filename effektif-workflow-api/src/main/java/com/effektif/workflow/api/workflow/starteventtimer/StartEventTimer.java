package com.effektif.workflow.api.workflow.starteventtimer;

import com.effektif.workflow.api.bpmn.BpmnReader;
import com.effektif.workflow.api.bpmn.XmlElement;
import com.effektif.workflow.api.json.TypeName;
import com.effektif.workflow.api.workflow.Timer;

/**
 * CumulusPro
 * Created by Jeroen on 14/06/16.
 */
@TypeName("startEventTimer")
public class StartEventTimer extends Timer {
    @Override
    public void readBpmn(BpmnReader r) {
        super.readBpmn(r);
    }
}
