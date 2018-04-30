package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.lbjava.learn.Softmax;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType.getNullTempRel;

public class TempRelLabelerLBJ implements TempRelLabeler {
    private boolean split_q1_q2;
    private Learner classifier_dist0,classifier_dist1;
    private Learner classifier_mod1_dist0,classifier_mod1_dist1;
    private Learner classifier_mod2_dist0,classifier_mod2_dist1;

    public TempRelLabelerLBJ(Learner classifier_dist0, Learner classifier_dist1) {
        split_q1_q2 = false;
        this.classifier_dist0 = classifier_dist0;
        this.classifier_dist1 = classifier_dist1;
    }

    public TempRelLabelerLBJ(Learner classifier_mod1_dist0, Learner classifier_mod2_dist0,Learner classifier_mod1_dist1, Learner classifier_mod2_dist1){
        split_q1_q2 = true;
        this.classifier_mod1_dist0 = classifier_mod1_dist0;
        this.classifier_mod1_dist1 = classifier_mod1_dist1;
        this.classifier_mod2_dist0 = classifier_mod2_dist0;
        this.classifier_mod2_dist1 = classifier_mod2_dist1;
    }

    @Override
    public TemporalRelType tempRelLabel(TemporalRelation_EE ee) {
        TemporalRelType ret = getNullTempRel();
        if(split_q1_q2){
            if (ee.getSentDiff() == 0 && classifier_mod1_dist0 != null && classifier_mod2_dist0 != null){
                String q1 = classifier_mod1_dist0.discreteValue(ee);
                String q2 = classifier_mod2_dist0.discreteValue(ee);
                double[] s1 = temporalScores2doubles(classifier_mod1_dist0.scores(ee),new String[]{"q1:yes","q1:no"},true);
                double[] s2 = temporalScores2doubles(classifier_mod2_dist0.scores(ee),new String[]{"q2:yes","q2:no"},true);
                temprelAnnotationReader.Q1_Q2_temprel tuple = new temprelAnnotationReader.Q1_Q2_temprel(q1.equals("q1:yes"),q2.equals("q2:yes"));
                ret = tuple.getRelType();
                ret.setScores(new double[]{s1[0]*s2[1],s1[1]*s2[0],s1[1]*s2[1],s1[0]*s2[0],0});
            }
            else if (ee.getSentDiff() == 1 && classifier_mod1_dist1 != null && classifier_mod2_dist1 != null){
                String q1 = classifier_mod1_dist1.discreteValue(ee);
                String q2 = classifier_mod2_dist1.discreteValue(ee);
                double[] s1 = temporalScores2doubles(classifier_mod1_dist1.scores(ee),new String[]{"q1:yes","q1:no"},true);
                double[] s2 = temporalScores2doubles(classifier_mod2_dist1.scores(ee),new String[]{"q2:yes","q2:no"},true);
                temprelAnnotationReader.Q1_Q2_temprel tuple = new temprelAnnotationReader.Q1_Q2_temprel(q1.equals("q1:yes"),q2.equals("q2:yes"));
                ret = tuple.getRelType();
                ret.setScores(new double[]{s1[0]*s2[1],s1[1]*s2[0],s1[1]*s2[1],s1[0]*s2[0],0});
            }
        }
        else {
            if (ee.getSentDiff() == 0 && classifier_dist0 != null) {
                ret = new TemporalRelType(classifier_dist0.discreteValue(ee));
                ret.setScores(temporalScores2doubles(classifier_dist0.scores(ee), TemporalRelType.relTypes.getAllNames(), true));
            }
            else if (ee.getSentDiff() == 1 && classifier_dist1 != null) {
                ret = new TemporalRelType(classifier_dist1.discreteValue(ee));
                ret.setScores(temporalScores2doubles(classifier_dist1.scores(ee),TemporalRelType.relTypes.getAllNames(),true));

            }
        }
        return ret;
    }
    private double[] temporalScores2doubles(ScoreSet scores, String[] allLabels, boolean norm){
        if(norm) {
            Softmax sm = new Softmax();
            scores = sm.normalize(scores);
        }
        int n = allLabels.length;
        double[] s = new double[n];
        for(int i=0;i<n;i++){
            try {
                s[i] = scores.get(allLabels[i]);
            }
            catch (Exception e){
                s[i] = 0;
            }
        }
        return s;
    }
}
