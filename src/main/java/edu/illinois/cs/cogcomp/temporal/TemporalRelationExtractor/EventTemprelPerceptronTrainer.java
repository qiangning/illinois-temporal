package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.readers.myDatasetLoader;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidation.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EventTemprelPerceptronTrainer extends CVWrapper_LBJ_Perceptron<TemporalRelation_EE> {
    private int window;
    private int sentDiff;
    private int clsMode;
    private double sr_standard;
    private String[] trainSet, testSet;
    public static String[] TEMP_LABEL_TO_IGNORE = new String[]{TemporalRelType.relTypes.VAGUE.getName(),TemporalRelType.relTypes.NULL.getName()};

    private static CommandLine cmd;

    public EventTemprelPerceptronTrainer(int seed, int totalFold, int labelMode, int clsMode, int window, int sentDiff, int evalMetric,
                                         String[] trainSet, String[] testSet) {
        super(seed, totalFold, evalMetric);
        this.window = window;
        this.sentDiff = sentDiff;
        this.clsMode = clsMode;
        this.trainSet = trainSet;
        this.testSet = testSet;
        System.out.println("Classifier Takes Mode "+clsMode);
        System.out.println("Label Takes Mode "+labelMode);

        TemporalRelation.setLabelMode(labelMode);
        LABEL_TO_IGNORE = TEMP_LABEL_TO_IGNORE;
        LEARNRATE = new double[]{0.001};
        THICKNESS = new double[]{0,1};
        SAMRATE = new double[]{0.1,0.2,0.3};
        ROUND = new double[]{50,100,200};
    }

    private List<TemporalRelation_EE> preprocess(List<myTemporalDocument> docList){
        List<TemporalRelation_EE> ret = new ArrayList<>();
        for(myTemporalDocument d:docList){
            d.extractAllFeats(window);
            ret.addAll(d.getGraph().getAllEERelations(sentDiff));
        }
        return ret;
    }
    @Override
    public void load() {
        try {
            myDatasetLoader myLoader = new myDatasetLoader();
            List<myTemporalDocument> allTrainingDocs = new ArrayList<>();//myLoader.getTimeBank();
            for(String str:trainSet){
                System.out.printf("Loading %s as training data...\n",str);
                allTrainingDocs.addAll(myLoader.getDataset(str));
            }
            List<myTemporalDocument> allTestingDocs = new ArrayList<>();//myLoader.getPlatinum();
            for(String str:testSet){
                System.out.printf("Loading %s as test data...\n",str);
                allTestingDocs.addAll(myLoader.getDataset(str));
            }
            trainingStructs = preprocess(allTrainingDocs);
            testStructs = preprocess(allTestingDocs);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<TemporalRelation_EE> SetLrThSrCls(double lr, double th, double sr, List<TemporalRelation_EE> slist) {
        ParamLBJ.EETempRelClassifierPerceptronParams.learningRate = lr;
        ParamLBJ.EETempRelClassifierPerceptronParams.thickness = th;
        Random rng = new Random(seed++);
        ListSampler<TemporalRelation_EE> listSampler = new ListSampler<>(
                element -> !element.getLabel().equals(TemporalRelType.relTypes.VAGUE.getName())
                        &&!element.getLabel().equals(TemporalRelType.relTypes.EQUAL.getName())
        );
        if(sr_standard==0d) {
            sr_standard = listSampler.autoSelectSamplingRate(slist);
            System.out.printf("Auto Selection of Sampling Rate: %.4f\n",sr_standard);
        }
        switch (clsMode) {
            case 0:
                classifier = new eeTempRelCls(modelPath, lexiconPath);
                break;
            default:
                System.out.println("Choosing default classifier 0");
                classifier = new eeTempRelCls(modelPath, lexiconPath);
        }
        return listSampler.ListSampling(slist,sr*sr_standard,rng);
    }

    @Override
    public String getLabel(TemporalRelation_EE temporalRelation_ee) {
        return temporalRelation_ee.getLabel();
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

        Option sentDiff = new Option("s", "sentDiff", true, "sentence distance");
        sentDiff.setRequired(true);
        options.addOption(sentDiff);

        Option labelMode = new Option("lm", "labelMode", true, "label mode (0: original labels; 1: Q1; 2: Q2)");
        labelMode.setRequired(false);
        options.addOption(labelMode);

        Option clsMode = new Option("cm", "clsMode", true, "classifier mode");
        clsMode.setRequired(false);
        options.addOption(clsMode);

        Option trainFiles = new Option("train", "trainFiles", true, "acronym for the training dataset");
        trainFiles.setRequired(false);
        options.addOption(trainFiles);

        Option testFiles = new Option("test", "testFiles", true, "acronym for the test dataset");
        testFiles.setRequired(false);
        options.addOption(testFiles);

        Option fold = new Option("f", "fold", true, "fold for cross-validation");
        fold.setRequired(false);
        options.addOption(fold);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("EventTemprelPerceptronTrainer", options);

            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception{
        cmdParser(args);
        String modelDir = cmd.getOptionValue("modelDir");
        String modelName = cmd.getOptionValue("modelName");
        int labelMode = Integer.valueOf(cmd.getOptionValue("labelMode","0"));
        int clsMode = Integer.valueOf(cmd.getOptionValue("clsMode","0"));
        int window = Integer.valueOf(cmd.getOptionValue("window"));
        int sentDiff = Integer.valueOf(cmd.getOptionValue("sentDiff"));
        int fold = Integer.valueOf(cmd.getOptionValue("fold","4"));
        String[] trainSet = cmd.getOptionValue("train","TimeBank_Ser,AQUAINT_Ser").split(",");
        String[] testSet = cmd.getOptionValue("test","PLATINUM_Ser").split(",");
        modelName += String.format("_sent%d_labelMode%d_clsMode%d_win%d",sentDiff,labelMode,clsMode,window);
        EventTemprelPerceptronTrainer exp = new EventTemprelPerceptronTrainer(0,fold,labelMode,clsMode,window,sentDiff,2,
                trainSet,testSet);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
