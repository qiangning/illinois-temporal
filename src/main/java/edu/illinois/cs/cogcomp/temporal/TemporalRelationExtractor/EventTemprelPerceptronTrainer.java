package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls2;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidation.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;
import util.TempLangMdl;

import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class EventTemprelPerceptronTrainer extends CVWrapper_LBJ_Perceptron<TemporalRelation_EE> {
    private int window;
    private int sentDiff;
    private int clsMode;
    private double sr_standard;
    public static String[] TEMP_LABEL_TO_IGNORE = new String[]{TemporalRelType.relTypes.VAGUE.getName(),TemporalRelType.relTypes.NULL.getName()};

    private static CommandLine cmd;

    public EventTemprelPerceptronTrainer(int seed, int totalFold, int labelMode, int clsMode, int window, int sentDiff, int evalMetric) {
        super(seed, totalFold, evalMetric);
        this.window = window;
        this.sentDiff = sentDiff;
        this.clsMode = clsMode;
        TemporalRelation.setLabelMode(labelMode);
        LABEL_TO_IGNORE = TEMP_LABEL_TO_IGNORE;
        LEARNRATE = new double[]{0.001};
        THICKNESS = new double[]{0,1};
        SAMRATE = new double[]{0.1,0.2,0.3};
        ROUND = new double[]{20,50,100};
    }

    private List<TemporalRelation_EE> preprocess(List<TemporalDocument> docList,
                                                 HashMap<String,HashMap<Integer,String>> axisMap,
                                                 HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap,
                                                 WNSim wnsim){

        List<TemporalRelation_EE> ret = new ArrayList<>();
        for(TemporalDocument d:docList){
            myTemporalDocument doc = new myTemporalDocument(d,1);
            String docid = doc.getDocid();
            if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                continue;
            doc.keepAnchorableEvents(axisMap.get(doc.getDocid()));
            doc.loadRelationsFromMap(relMap.get(doc.getDocid()),0);
            List<EventTemporalNode> events = doc.getEventList();
            for(EventTemporalNode e:events){
                e.extractPosLemmaWin(window);
                e.extractSynsets(wnsim);
            }
            ret.addAll(doc.getGraph().getAllEERelations(sentDiff));
        }
        return ret;
    }
    @Override
    public void load() {
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            WNSim wnsim = WNSim.getInstance(rm.getString("WordNet_Dir"));
            List<TemporalDocument> allTrainingDocs = TempEval3Reader.deserialize(rm.getString("TimeBank_Ser"));
            allTrainingDocs.addAll(TempEval3Reader.deserialize(rm.getString("AQUAINT_Ser")));
            List<TemporalDocument> allTestingDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
            HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));

            String lm_path = rm.getString("TemProb_Dir");
            TempLangMdl tempLangMdl = TempLangMdl.getInstance(lm_path);

            trainingStructs = preprocess(allTrainingDocs,axisMap,relMap,wnsim);
            testStructs = preprocess(allTestingDocs,axisMap,relMap,wnsim);
            for(TemporalRelation_EE tmp:trainingStructs) {
                tmp.extractSignalWords();
                tmp.readCorpusStats(tempLangMdl.tempLangMdl);
            }
            for(TemporalRelation_EE tmp:testStructs) {
                tmp.extractSignalWords();
                tmp.readCorpusStats(tempLangMdl.tempLangMdl);
            }
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
            case 2:
                classifier = new eeTempRelCls2(modelPath, lexiconPath);
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
        labelMode.setRequired(true);
        options.addOption(labelMode);

        Option clsMode = new Option("cm", "clsMode", true, "classifier mode");
        clsMode.setRequired(false);
        options.addOption(clsMode);

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
        int labelMode = Integer.valueOf(cmd.getOptionValue("labelMode"));
        int clsMode = Integer.valueOf(cmd.getOptionValue("clsMode","0"));
        int window = Integer.valueOf(cmd.getOptionValue("window"));
        int sentDiff = Integer.valueOf(cmd.getOptionValue("sentDiff"));
        modelName += String.format("_sent%d_labelMode%d_clsMode%d_win%d",sentDiff,labelMode,clsMode,window);
        EventTemprelPerceptronTrainer exp = new EventTemprelPerceptronTrainer(0,4,labelMode,clsMode,window,sentDiff,2);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
