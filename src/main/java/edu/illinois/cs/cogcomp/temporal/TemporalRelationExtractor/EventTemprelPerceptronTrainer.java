package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;

import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class EventTemprelPerceptronTrainer extends CVWrapper_LBJ_Perceptron<TemporalRelation_EE> {
    private int window;
    private int sentDiff;
    public static String[] TEMP_LABEL_TO_IGNORE = new String[]{TemporalRelType.relTypes.VAGUE.getName()};

    private static CommandLine cmd;

    public EventTemprelPerceptronTrainer(int seed, int totalFold, int mode, int window, int sentDiff, int evalMetric) {
        super(seed, totalFold, evalMetric);
        this.window = window;
        this.sentDiff = sentDiff;
        TemporalRelation.setLabelMode(mode);
        LABEL_TO_IGNORE = TEMP_LABEL_TO_IGNORE;
        LEARNRATE = new double[]{0.0001,0.001,0.01};
        THICKNESS = new double[]{0,1};
        SAMRATE = new double[]{1};
        ROUND = new double[]{5,10,20};
    }

    @Override
    public void load() {
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            WNSim wnsim = WNSim.getInstance(rm.getString("WordNet_Dir"));
            String dir = rm.getString("TimeBank_Ser");
            List<TemporalDocument> allDocs = TempEval3Reader.deserialize(dir);
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
            HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));
            List<myTemporalDocument> myAllDocs = new ArrayList<>();
            for(TemporalDocument d:allDocs){
                myTemporalDocument doc = new myTemporalDocument(d,1);
                String docid = doc.getDocid();
                if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                    continue;
                doc.keepAnchorableEvents(axisMap.get(doc.getDocid()));
                doc.loadRelationsFromMap(relMap.get(doc.getDocid()),0);
                myAllDocs.add(doc);
                List<EventTemporalNode> events = doc.getEventList();
                for(EventTemporalNode e:events){
                    e.extractPosLemmaWin(window);
                    e.extractSynsets(wnsim);
                }
            }

            trainingStructs = new ArrayList<>();
            testStructs = new ArrayList<>();
            int testDocSize = 20;
            for(int i=0;i<testDocSize;i++){
                myTemporalDocument doc = myAllDocs.get(i);
                testStructs.addAll(doc.getGraph().getAllEERelations(sentDiff));
            }
            for(int i=testDocSize;i<myAllDocs.size();i++){
                myTemporalDocument doc = myAllDocs.get(i);
                trainingStructs.addAll(doc.getGraph().getAllEERelations(sentDiff));
            }
            for(TemporalRelation_EE tmp:trainingStructs)
                tmp.extractSignalWords();
            for(TemporalRelation_EE tmp:testStructs)
                tmp.extractSignalWords();
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
        classifier = new eeTempRelCls(modelPath,lexiconPath);
        return listSampler.ListSampling(slist,sr,rng);
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

        Option mode = new Option("m", "mode", true, "label mode (0: original labels; 1: Q1; 2: Q2)");
        mode.setRequired(true);
        options.addOption(mode);

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
        int mode = Integer.valueOf(cmd.getOptionValue("mode"));
        int window = Integer.valueOf(cmd.getOptionValue("window"));
        int sentDiff = Integer.valueOf(cmd.getOptionValue("sentDiff"));
        modelName += "_mod"+mode;
        modelName += "_win"+window;
        modelName += "_sent"+sentDiff;
        EventTemprelPerceptronTrainer exp = new EventTemprelPerceptronTrainer(0,5,mode,window,sentDiff,2);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
