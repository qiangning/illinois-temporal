package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.eventDetector;
import edu.illinois.cs.cogcomp.temporal.utils.CVWrapper_LBJ_Perceptron;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.*;

public class EventAxisPerceptronTrainer extends CVWrapper_LBJ_Perceptron<EventTokenCandidate> {
    private int window;
    public static String[] AXIS_LABEL_TO_IGNORE = new String[]{LABEL_NOT_ON_ANY_AXIS};

    private static CommandLine cmd;

    public EventAxisPerceptronTrainer(int seed, int totalFold, int window, int evalMetric) {
        super(seed, totalFold,evalMetric);
        this.window = window;
        LABEL_TO_IGNORE = AXIS_LABEL_TO_IGNORE;
        LEARNRATE = new double[]{0.0001,0.0002};
        THICKNESS = new double[]{0,1};
        SAMRATE = new double[]{1};
        ROUND = new double[]{5,10,20};
    }

    @Override
    public void load(){
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            List<TemporalDocument> allDocs = TempEval3Reader.deserialize(rm.getString("TimeBank_Ser"));
            allDocs.addAll(TempEval3Reader.deserialize(rm.getString("AQUAINT_Ser")));
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));// docid-->eventid-->axis_label
            // convert eventid in axisMap to tokenId
            for(int i=0;i<allDocs.size();i++){
                String docid = allDocs.get(i).getDocID();
                if(!axisMap.containsKey(docid)) continue;
                HashMap<Integer,Integer> index2TokId = eventIndex2TokId(allDocs.get(i));
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
                myTemporalDocument doc = new myTemporalDocument(allDocs.get(i));
                if(!axisMap.containsKey(doc.getDocid())) continue;
                testStructs.addAll(doc.generateAllEventTokenCandidates(window,axisMap.get(doc.getDocid())));
            }
            for(int i=testDocSize;i<allDocs.size();i++){
                myTemporalDocument doc = new myTemporalDocument(allDocs.get(i));
                if(!axisMap.containsKey(doc.getDocid())) continue;
                trainingStructs.addAll(doc.generateAllEventTokenCandidates(window,axisMap.get(doc.getDocid())));
            }
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
                element -> !element.getLabel().equals(LABEL_NOT_ON_ANY_AXIS)
        );
        classifier = new eventDetector(modelPath,lexiconPath);
        return listSampler.ListSampling(slist,sr,rng);
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
        exp.setModelPath(modelDir,modelName);
        StandardExperiment(exp);
    }
}
