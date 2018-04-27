package edu.illinois.cs.cogcomp.temporal.utils.CrossValidation;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class CVWrapper_LBJ_Perceptron<LearningStruct> extends CVWRapper_LBJ<LearningStruct> {
    protected static double[] LEARNRATE,THICKNESS,SAMRATE,ROUND;

    public CVWrapper_LBJ_Perceptron(int seed, int totalFold, int evalMetric) {
        super(seed, totalFold, evalMetric);
    }

    public abstract List<LearningStruct> SetLrThSrCls(double lr, double th, double sr,List<LearningStruct> slist);
    @Override
    public void learn(List<LearningStruct> slist, double[] param, int seed) {
        double lr = param[0];
        double th = param[1];
        double sr = param[2];
        int round = (int) Math.round(param[3]);
        List<LearningStruct> slist_sample = SetLrThSrCls(lr,th,sr,slist);
        classifier.forget();
        classifier.beginTraining();
        for(int iter=0;iter<round;iter++){
            Collections.shuffle(slist_sample, new Random(seed++));
            for(LearningStruct ee:slist_sample){
                try{
                    classifier.learn(ee);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        classifier.doneLearning();
    }

    @Override
    public void setParams2tune() {
        params2tune = new double[LEARNRATE.length*THICKNESS.length* SAMRATE.length*ROUND.length][4];
        int cnt = 0;
        for(double lr:LEARNRATE){
            for(double th:THICKNESS){
                for(double nvsr: SAMRATE){
                    for(double r:ROUND){
                        params2tune[cnt] = new double[]{lr,th,nvsr,r};
                        cnt++;
                    }
                }
            }
        }
    }
}
