package com.effektif.workflow.impl.intermediate;

import com.effektif.workflow.api.activities.NoneTask;
import com.effektif.workflow.api.bpmn.BpmnElement;
import com.effektif.workflow.api.bpmn.BpmnReader;
import com.effektif.workflow.api.bpmn.XmlElement;
import com.effektif.workflow.api.json.TypeName;
import com.effektif.workflow.api.workflow.Timer;

/**
 * CumulusPro
 * Created by Jeroen on 16/06/16.
 */
@TypeName("intermediateThrowEvent")
@BpmnElement("intermediateThrowEvent")
public class IntermediateThrowEvent extends NoneTask {
    @Override
    public void readBpmn(BpmnReader r) {
        super.readBpmn(r);
        for (XmlElement element : r.readElementsBpmn("timerEventDefinition")) {

            r.startElement(element);

            Timer timer = new IntermediateThrowEventTimer();
            timer.readBpmn(r);
            this.timer(timer);

            r.endElement();
        }
    }
}
