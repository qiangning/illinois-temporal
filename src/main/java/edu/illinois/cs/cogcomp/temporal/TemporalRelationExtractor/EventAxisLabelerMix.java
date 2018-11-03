package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.lbjava.learn.Learner;

import static edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.NegationRules.NEGATION;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;

public class EventAxisLabelerMix extends EventAxisLabelerLBJ{
    public Learner classifier;
    private NegationRules negationRules = new NegationRules();
    private HypothesisRules hypothesisRules = new HypothesisRules();
    private IntentionRules intentionRules = new IntentionRules();


    public EventAxisLabelerMix(Learner classifier) {
        super(classifier);
    }

    @Override
    public String axisLabel(EventTokenCandidate etc) {
        if(negationRules.negationRule(etc).equals(NEGATION)) return LABEL_NOT_ON_ANY_AXIS;
        if(intentionRules.intentionRule_to(etc).equals(IntentionRules.INTENTION)) return LABEL_NOT_ON_ANY_AXIS;
        if(hypothesisRules.hypothesisRule(etc).equals(HypothesisRules.HYPOTHESIS)) return LABEL_NOT_ON_ANY_AXIS;
        return classifier!=null?
                classifier.discreteValue(etc):LABEL_NOT_ON_ANY_AXIS;
    }
}
