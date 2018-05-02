package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.*;
import edu.illinois.cs.cogcomp.temporal.utils.CrossValidation.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.*;

public class EventAxisPerceptronTrainer extends CVWrapper_LBJ_Perceptron<EventTokenCandidate> {
    private int window;
    private int clsMode;
    public static String[] AXIS_LABEL_TO_IGNORE = new String[]{};

    private static CommandLine cmd;

    public EventAxisPerceptronTrainer(int seed, int totalFold, int window, int clsMode, int evalMetric) {
        super(seed, totalFold,evalMetric);
        this.window = window;
        this.clsMode = clsMode;
        System.out.println("Classifier Takes Mode "+clsMode);
        LABEL_TO_IGNORE = AXIS_LABEL_TO_IGNORE;
        LEARNRATE = new double[]{0.0001};
        THICKNESS = new double[]{0,0.1};
        SAMRATE = new double[]{1};
        ROUND = new double[]{5,10,20};
    }
    public static List<EventTokenCandidate> preprocess(List<TemporalDocument> docList,HashMap<String,HashMap<Integer,String>> axisMap, int window){
        // axisMap: docid-->index in doc-->raw axis name from CF
        HashMap<String,HashMap<Integer,String>> newAxisMap = new HashMap<>();
        // convert eventid in axisMap to tokenId
        for(int i=0;i<docList.size();i++){
            String docid = docList.get(i).getDocID();
            if(!axisMap.containsKey(docid)) continue;
            HashMap<Integer,Integer> index2TokId = eventIndex2TokId(docList.get(i));
            HashMap<Integer,String> tmpMap = axisMap.get(docid);
            HashMap<Integer,String> tmpMap2 = new HashMap<>();
            for(int eventid:tmpMap.keySet()){
                // convert labels in axisMap
                tmpMap2.put(index2TokId.get(eventid),axis_label_conversion(tmpMap.get(eventid)));
            }
            newAxisMap.put(docid,tmpMap2);
        }
        List<EventTokenCandidate> ret = new ArrayList<>();
        for(int i=0;i<docList.size();i++){
            if(!newAxisMap.containsKey(docList.get(i).getDocID())) continue;
            myTemporalDocument doc = new myTemporalDocument(docList.get(i),1);
            ret.addAll(doc.generateAllEventTokenCandidates(window,newAxisMap.get(doc.getDocid())));
        }
        return ret;
    }

    @Override
    public void load(){
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            List<TemporalDocument> allTrainingDocs = TempEval3Reader.deserialize(rm.getString("TimeBank_Ser"));
            allTrainingDocs.addAll(TempEval3Reader.deserialize(rm.getString("AQUAINT_Ser")));
            List<TemporalDocument> allTestingDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));// docid-->eventid-->axis_label
            trainingStructs = preprocess(allTrainingDocs,axisMap,window);
            testStructs = preprocess(allTestingDocs,axisMap,window);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<EventTokenCandidate> SetLrThSrCls(double lr, double th, double sr, List<EventTokenCandidate> slist) {
        ParamLBJ.EventDetectorPerceptronParams.learningRate = lr;
        ParamLBJ.EventDetectorPerceptronParams.thickness = th;
        Random rng = new Random(seed++);
        ListSampler<EventTokenCandidate> listSampler = new ListSampler<>(
                element -> element.getLabel().equals(LABEL_ON_MAIN_AXIS)
        );
        double sr_standard = listSampler.autoSelectSamplingRate(slist);
        switch (clsMode) {
            case 0:
                classifier = new eventDetector(modelPath, lexiconPath);
                break;
            case 2:
                classifier = new eventDetector2(modelPath, lexiconPath);
                break;
            case 3:
                classifier = new eventDetector3(modelPath, lexiconPath);
                break;
            case 4:
                classifier = new eventDetector4(modelPath, lexiconPath);
                break;
            case 5:
                classifier = new eventDetector5(modelPath, lexiconPath);
                break;
            default:
                System.out.println("Choosing default classifier 0");
                classifier = new eventDetector(modelPath, lexiconPath);
        }
        return listSampler.ListSampling(slist,sr*sr_standard,rng);
    }

    @Override
    public String getLabel(EventTokenCandidate eventTokenCandidate) {
        return eventTokenCandidate.getLabel();
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

        Option clsMode = new Option("cm", "clsMode", true, "classifier mode");
        clsMode.setRequired(false);
        options.addOption(clsMode);

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
        int clsMode = Integer.valueOf(cmd.getOptionValue("clsMode","0"));
        modelName += String.format("_win%d_cls%d",window,clsMode);
        EventAxisPerceptronTrainer exp = new EventAxisPerceptronTrainer(0,4,window,clsMode,2);
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
