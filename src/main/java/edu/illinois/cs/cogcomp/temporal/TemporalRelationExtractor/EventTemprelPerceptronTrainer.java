package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.EventTemporalNode;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidationWrapper_LbjavaLearner;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;

import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class EventTemprelPerceptronTrainer extends CrossValidationWrapper_LbjavaLearner<TemporalRelation_EE> {
    private int window;
    private int sentDiff;
    private static double[] LEARNRATE = new double[]{0.0001,0.0002};
    private static double[] THICKNESS = new double[]{0,1};
    private static double[] SAMRATE = new double[]{1};
    private static double[] ROUND = new double[]{5,10,20};
    public static String[] TEMP_LABEL_TO_IGNORE = new String[]{TemporalRelType.relTypes.VAGUE.getName()};

    private static CommandLine cmd;

    public EventTemprelPerceptronTrainer(int seed, int totalFold, int window, int sentDiff, int evalMetric) {
        super(seed, totalFold,evalMetric);
        this.window = window;
        this.sentDiff = sentDiff;
        LABEL_TO_IGNORE = TEMP_LABEL_TO_IGNORE;
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
    public void learn(List<TemporalRelation_EE> slist, double[] param, int seed) {
        double lr = param[0];
        double th = param[1];
        double sr = param[2];
        int round = (int) Math.round(param[3]);
        Random rng = new Random(seed++);

        ListSampler<TemporalRelation_EE> listSampler = new ListSampler<>(
                element -> !element.getLabel().equals(TemporalRelType.relTypes.VAGUE.getName())
                &&!element.getLabel().equals(TemporalRelType.relTypes.EQUAL.getName())
        );
        List<TemporalRelation_EE> slist_sample = listSampler.ListSampling(slist,sr,rng);

        ParamLBJ.EETempRelClassifierPerceptronParams.learningRate = lr;
        ParamLBJ.EETempRelClassifierPerceptronParams.thickness = th;
        classifier = new eeTempRelCls(modelPath,lexiconPath);
        classifier.forget();
        classifier.beginTraining();
        for(int iter=0;iter<round;iter++){
            Collections.shuffle(slist_sample, new Random(seed++));
            for(TemporalRelation_EE ee:slist_sample){
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
        int window = Integer.valueOf(cmd.getOptionValue("window"));
        int sentDiff = Integer.valueOf(cmd.getOptionValue("sentDiff"));
        modelName += "_win"+window;
        modelName += "_sent"+sentDiff;
        EventTemprelPerceptronTrainer exp = new EventTemprelPerceptronTrainer(0,5,window,sentDiff,2);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
