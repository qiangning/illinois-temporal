package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.lbjava.learn.Learner;

import static edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.HypothesisRules.hypothesisRule;
import static edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.IntentionRules.intentionRule_to;
import static edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.NegationRules.negationRule;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;

public class EventAxisLabelerMix extends EventAxisLabelerLBJ{
    public static int window;


    public EventAxisLabelerMix(Learner classifier) {
        super(classifier);
    }

    @Override
    public String axisLabel(EventTokenCandidate etc) {
        if(negationRule(etc).equals(NegationRules.NEGATION))
            return LABEL_NOT_ON_ANY_AXIS;
        if(intentionRule_to(etc,window).equals(IntentionRules.INTENTION))
            return LABEL_NOT_ON_ANY_AXIS;
        if(hypothesisRule(etc).equals(HypothesisRules.HYPOTHESIS))
            return LABEL_NOT_ON_ANY_AXIS;
        return classifier!=null?
                classifier.discreteValue(etc):LABEL_NOT_ON_ANY_AXIS;
    }
}
