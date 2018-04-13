package edu.illinois.cs.cogcomp.temporal.eventAxisDetector;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.eventDetector;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidationWrapper;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.*;

public class EventAxisPerceptronTrainer extends CrossValidationWrapper<EventTokenCandidate>{
    private List<EventTokenCandidate> testStructs;
    private int window;
    private String modelPath, lexiconPath;
    private int evalMetric = 2;//0:prec. 1: recall. 2: f1
    private Learner classifier;
    private static double[] LEARNRATE = new double[]{0.0001,0.0002};
    private static double[] THICKNESS = new double[]{0,1};
    private static double[] NEGVAGSAMRATE= new double[]{0.3,0.5,0.7};
    private static double[] ROUND = new double[]{5,10,20};
    private static String[] LABEL_TO_IGNORE = new String[]{LABEL_NOT_ON_ANY_AXIS};

    private static CommandLine cmd;

    public EventAxisPerceptronTrainer(int seed, int totalFold, int window, int evalMetric) {
        super(seed, totalFold);
        this.window = window;
        this.evalMetric = evalMetric;
    }

    public void setModelPath(String dir, String name) {
        modelPath = dir+ File.separator+name+".lc";
        lexiconPath = dir+File.separator+name+".lex";
    }

    @Override
    public void load(){
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            String dir = rm.getString("TimeBank_Ser");
            List<TemporalDocument> timebank = TempEval3Reader.deserialize(dir);
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_TimeBank_Axis"));// docid-->eventid-->axis_label
            // convert eventid in axisMap to tokenId
            for(int i=0;i<timebank.size();i++){
                String docid = timebank.get(i).getDocID();
                if(!axisMap.containsKey(docid)) continue;
                HashMap<Integer,Integer> index2TokId = eventIndex2TokId(timebank.get(i));
                HashMap<Integer,String> tmpMap = axisMap.get(docid);
                HashMap<Integer,String> tmpMap2 = new HashMap<>();
                for(int eventid:tmpMap.keySet()){
                    tmpMap2.put(index2TokId.get(eventid),tmpMap.get(eventid));
                }
                axisMap.put(docid,tmpMap2);
            }
            // convert labels in axisMap
            for(String docid:axisMap.keySet()){
                for(int id:axisMap.get(docid).keySet()){
                    String label = axisMap.get(docid).get(id);
                    String label_new = axis_label_conversion(label);
                    axisMap.get(docid).put(id,label_new);
                }
            }

            trainingStructs = new ArrayList<>();
            testStructs = new ArrayList<>();
            int testDocSize = 20;
            for(int i=0;i<testDocSize;i++){
                myTemporalDocument doc = new myTemporalDocument(timebank.get(i));
                if(!axisMap.containsKey(doc.getDocid())) continue;
                testStructs.addAll(EventTokenCandidate.generateAllEventTokenCandidates(doc,window,axisMap.get(doc.getDocid())));
            }
            for(int i=testDocSize;i<timebank.size();i++){
                myTemporalDocument doc = new myTemporalDocument(timebank.get(i));
                if(!axisMap.containsKey(doc.getDocid())) continue;
                trainingStructs.addAll(EventTokenCandidate.generateAllEventTokenCandidates(doc,window,axisMap.get(doc.getDocid())));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void learn(List<EventTokenCandidate> slist, double[] param, int seed) {
        double lr = param[0];
        double th = param[1];
        double nvsr = param[2];
        int round = (int) Math.round(param[3]);
        List<EventTokenCandidate> slist_negSam = new ArrayList<>();
        Random rng = new Random(seed++);
        for(EventTokenCandidate st:slist){
            if(!st.getLabel().equals(LABEL_NOT_ON_ANY_AXIS))
                slist_negSam.add(st);
            else {
                if (nvsr <= 1) {
                    if (rng.nextDouble() <= nvsr)
                        slist_negSam.add(st);
                } else {
                    double tmp = nvsr;
                    for (; tmp > 1; tmp--) {
                        slist_negSam.add(st);
                    }
                    if (rng.nextDouble() <= tmp)
                        slist_negSam.add(st);
                }
            }
        }
        // TO-DO: set parameters of classifier
        ParamLBJ.EventDetectorPerceptronParams.learningRate = lr;
        ParamLBJ.EventDetectorPerceptronParams.thickness = th;
        classifier = new eventDetector(modelPath,lexiconPath);
        classifier.forget();
        classifier.beginTraining();
        for(int iter=0;iter<round;iter++){
            Collections.shuffle(slist_negSam, new Random(seed++));
            for(EventTokenCandidate etc:slist_negSam){
                try{
                    classifier.learn(etc);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        classifier.doneLearning();
    }

    @Override
    public double evaluate(List<EventTokenCandidate> slist, int verbose) {
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        timer.start();
        for(EventTokenCandidate etc:slist){
            String p = classifier.discreteValue(etc);
            String l = etc.getLabel();
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

    @Override
    public void setParams2tune() {
        params2tune = new double[LEARNRATE.length*THICKNESS.length*NEGVAGSAMRATE.length*ROUND.length][4];
        int cnt = 0;
        for(double lr:LEARNRATE){
            for(double th:THICKNESS){
                for(double nvsr:NEGVAGSAMRATE){
                    for(double r:ROUND){
                        params2tune[cnt] = new double[]{lr,th,nvsr,r};
                        cnt++;
                    }
                }
            }
        }
    }
    public double evaluateTest(){
        System.out.println("-------------------");
        System.out.println("Evaluating TestSet...");
        return evaluate(testStructs,1);
    }
    public void saveClassifier(){
        classifier.write(modelPath,lexiconPath);
    }
    public static void cmdParser(String[] args) {
        Options options = new Options();

        Option modelDir = new Option("d", "modelDir", true, "model output directory");
        modelDir.setRequired(true);
        options.addOption(modelDir);

        Option modelName = new Option("n", "modelName", true, "model name");
        modelName.setRequired(true);
        options.addOption(modelName);

        Option window = new Option("w", "window", true, "window size");
        window.setRequired(true);
        options.addOption(window);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("EventAxisPerceptronTrainer", options);

            System.exit(1);
        }
    }
    public static void main(String[] args) throws Exception{
        cmdParser(args);
        String modelDir = cmd.getOptionValue("modelDir");
        String modelName = cmd.getOptionValue("modelName");
        int window = Integer.valueOf(cmd.getOptionValue("window"));
        modelName += "_win"+window;
        EventAxisPerceptronTrainer exp = new EventAxisPerceptronTrainer(0,5,window,2);
        exp.load();
        exp.setModelPath(modelDir,modelName);
        exp.myParamTuner();
        exp.retrainUsingBest();
        exp.evaluateTest();
        exp.saveClassifier();
    }
}
