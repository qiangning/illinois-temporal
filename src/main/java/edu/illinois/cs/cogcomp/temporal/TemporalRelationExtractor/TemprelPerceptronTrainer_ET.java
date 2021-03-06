package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls_ET.etTempRelCls;
import edu.illinois.cs.cogcomp.temporal.readers.myDatasetLoader;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidation.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.illinois.cs.cogcomp.temporal.utils.myLogFormatter;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TemprelPerceptronTrainer_ET extends CVWrapper_LBJ_Perceptron<TemporalRelation_ET> {
    private int window;
    private int sentDiff;
    private int clsMode;
    private double sr_standard;
    private String[] trainSet, testSet;
    private boolean autoAdjustSamplingRate;
    public static String[] TEMP_LABEL_TO_IGNORE = new String[]{TemporalRelType.relTypes.VAGUE.getName(),TemporalRelType.relTypes.NULL.getName()};

    private static CommandLine cmd;

    public TemprelPerceptronTrainer_ET(int seed, int totalFold, int labelMode, boolean autoAdjustSamplingRate, int clsMode, int window, int sentDiff, int evalMetric,
                                       String[] trainSet, String[] testSet) {
        super(seed, totalFold, evalMetric);
        this.autoAdjustSamplingRate = autoAdjustSamplingRate;
        this.window = window;
        this.sentDiff = sentDiff;
        this.clsMode = clsMode;
        this.trainSet = trainSet;
        this.testSet = testSet;
        System.out.println("Classifier Takes Mode "+clsMode);
        System.out.println("Label Takes Mode "+labelMode);

        TemporalRelation.setLabelMode(labelMode);
        LABEL_TO_IGNORE = TEMP_LABEL_TO_IGNORE;
        LEARNRATE = ParamLBJ.ETTempRelClassifierPerceptronParams.LEARNRATE;
        THICKNESS = ParamLBJ.ETTempRelClassifierPerceptronParams.THICKNESS;
        SAMRATE = ParamLBJ.ETTempRelClassifierPerceptronParams.SAMRATE;
        ROUND = ParamLBJ.ETTempRelClassifierPerceptronParams.ROUND;

        System.out.println(myLogFormatter.fullBlockLog("autoSelectSamplingRate:"+this.autoAdjustSamplingRate));
    }

    private List<TemporalRelation_ET> preprocess(List<myTemporalDocument> docList){
        List<TemporalRelation_ET> ret = new ArrayList<>();
        for(myTemporalDocument d:docList){
            d.extractAllFeats(window);
            ret.addAll(d.getGraph().getAllETRelations(sentDiff));
        }
        return ret;
    }
    @Override
    public void load() {
        try {
            myDatasetLoader myLoader = new myDatasetLoader();
            List<myTemporalDocument> allTrainingDocs = new ArrayList<>();
            for(String str:trainSet){
                System.out.printf("Loading %s as training data...\n",str);
                allTrainingDocs.addAll(myLoader.getDataset(str));
            }
            List<myTemporalDocument> allTestingDocs = new ArrayList<>();
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
    public List<TemporalRelation_ET> SetLrThSrCls(double lr, double th, double sr, List<TemporalRelation_ET> slist) {
        ParamLBJ.ETTempRelClassifierPerceptronParams.learningRate = lr;
        ParamLBJ.ETTempRelClassifierPerceptronParams.thickness = th;
        Random rng = new Random(seed++);
        ListSampler<TemporalRelation_ET> listSampler = new ListSampler<>(
                element -> element.getLabel().equals(TemporalRelType.relTypes.VAGUE.getName())
        );
        if(autoAdjustSamplingRate) {
            if (sr_standard == 0d) {
                sr_standard = listSampler.autoSelectSamplingRate(slist);
                System.out.printf("Auto Selection of Sampling Rate: %.4f\n", sr_standard);
            }
        }
        else{
            sr_standard = 1d;
        }
        switch (clsMode) {
            case 0:
                classifier = new etTempRelCls(modelPath, lexiconPath);
                break;
            default:
                System.out.println("Choosing default classifier 0");
                classifier = new etTempRelCls(modelPath, lexiconPath);
        }
        //System.out.println(myLogFormatter.fullBlockLog("Sampling Rate: "+sr*sr_standard));
        return listSampler.ListSampling(slist,sr*sr_standard,rng);
    }

    @Override
    public String getLabel(TemporalRelation_ET temporalRelation_ee) {
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

        Option autoSelectSamplingRate = new Option("as", "autoSelectSamplingRate", false, "auto selection of sampling rate");
        autoSelectSamplingRate.setRequired(false);
        options.addOption(autoSelectSamplingRate);

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
            formatter.printHelp("TemprelPerceptronTrainer_ET", options);

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
        boolean autoSelectSamplingRate = cmd.hasOption("autoSelectSamplingRate");
        String[] trainSet = cmd.getOptionValue("train","TimeBank_Ser,AQUAINT_Ser").split(",");
        String[] testSet = cmd.getOptionValue("test","PLATINUM_Ser").split(",");
        modelName += String.format("_sent%d_labelMode%d_clsMode%d_win%d",sentDiff,labelMode,clsMode,window);
        TemprelPerceptronTrainer_ET exp = new TemprelPerceptronTrainer_ET(0,fold,labelMode,autoSelectSamplingRate,clsMode,window,sentDiff,2,
                trainSet,testSet);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
