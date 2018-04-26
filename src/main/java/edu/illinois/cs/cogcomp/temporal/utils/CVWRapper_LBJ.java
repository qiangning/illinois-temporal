package edu.illinois.cs.cogcomp.temporal.utils;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;

import java.io.File;
import java.util.List;

public abstract class CVWRapper_LBJ<LearningStruct> extends CrossValidationWrapper<LearningStruct> {
    protected Learner classifier;
    protected List<LearningStruct> testStructs;
    protected int evalMetric = 2;//0:prec. 1: recall. 2: f1
    protected String modelPath, lexiconPath;
    protected static String[] LABEL_TO_IGNORE = new String[]{};

    public CVWRapper_LBJ(int seed, int totalFold, int evalMetric) {
        super(seed, totalFold);
        this.evalMetric = evalMetric;
    }
    public void setModelPath(String dir, String name) {
        IOUtils.mkdir(dir);
        modelPath = dir+ File.separator+name+".lc";
        lexiconPath = dir+File.separator+name+".lex";
    }

    public abstract String getLabel(LearningStruct learningStruct);
    @Override
    public double evaluate(List<LearningStruct> slist, int verbose) {
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        timer.start();
        for(LearningStruct learningStruct:slist){
            String p = classifier.discreteValue(learningStruct);
            String l = getLabel(learningStruct);
            evaluator.addPredGoldLabels(p,l);
        }
        timer.end();
        if(verbose>0) {
            evaluator.printPrecisionRecall(LABEL_TO_IGNORE);
        }
        double res;
        switch(evalMetric){
            case 0:
                res = evaluator.getResultStruct(LABEL_TO_IGNORE).prec;
                break;
            case 1:
                res = evaluator.getResultStruct(LABEL_TO_IGNORE).rec;
                break;
            case 2:
                res = evaluator.getResultStruct(LABEL_TO_IGNORE).f;
                break;
            default:
                res = evaluator.getResultStruct(LABEL_TO_IGNORE).f;
        }
        return res;
    }
    public double evaluateTest(){
        System.out.println("-------------------");
        System.out.println("Evaluating TestSet...");
        return evaluate(testStructs,1);
    }
    public void saveClassifier(){
        classifier.write(modelPath,lexiconPath);
    }

    public static void StandardExperiment(CVWRapper_LBJ exp){
        // remember to setup model names before calling this
        exp.load();
        exp.myParamTuner();
        exp.retrainUsingBest();
        exp.evaluateTest();
        exp.saveClassifier();
    }
}
