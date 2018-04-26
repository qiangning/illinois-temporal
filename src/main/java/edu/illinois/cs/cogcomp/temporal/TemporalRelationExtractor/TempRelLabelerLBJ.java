package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType.getNullTempRel;

public class TempRelLabelerLBJ implements TempRelLabeler {
    private Learner classifier_dist0,classifier_dist1;

    public TempRelLabelerLBJ(Learner classifier_dist0, Learner classifier_dist1) {
        this.classifier_dist0 = classifier_dist0;
        this.classifier_dist1 = classifier_dist1;
    }

    @Override
    public TemporalRelType tempRelLabel(TemporalRelation_EE ee) {
        TemporalRelType ret = getNullTempRel();
        if(ee.getSentDiff()==0&&classifier_dist0!=null)
            ret = new TemporalRelType(classifier_dist0.discreteValue(ee));
        else if(ee.getSentDiff()==1&&classifier_dist1!=null)
            ret = new TemporalRelType(classifier_dist1.discreteValue(ee));
        return ret;
    }
}
