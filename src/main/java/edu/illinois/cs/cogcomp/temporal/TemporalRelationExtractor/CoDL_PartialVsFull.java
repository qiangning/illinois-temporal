package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.temporal.configurations.ParamLBJ;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.readers.myDatasetLoader;
import edu.illinois.cs.cogcomp.temporal.utils.CoDL.CoDLWrapper_LBJ;
import edu.illinois.cs.cogcomp.temporal.utils.CoDL.TempRelLabelerMultiLBJ;
import edu.illinois.cs.cogcomp.temporal.utils.ListSampler;
import edu.illinois.cs.cogcomp.temporal.utils.myLogFormatter;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class CoDL_PartialVsFull extends CoDLWrapper_LBJ<myTemporalDocument,TemporalRelation_EE> {
    private ResourceManager rm;
    public double learningRate=0.001, thickness=1, samplingRate=1, learningRound=200;
    private double sr_standard;
    private boolean ilp=true,respectExistingTempRelsInCoDL=true,respectAsHardConstraints=false;
    private double graphSamplingRate = 1;
    private int graphSamplingMode = 1;// 0,2,3: sample edges in graph. 1: sample docs.

    private static CommandLine cmd;
    private static boolean debug = false;


    public CoDL_PartialVsFull(int maxRound, int seed, double graphSamplingRate, int graphSamplingMode,
                       boolean OneMdlOrTwoMdl, double lambda, boolean forceUpdate, boolean saveCache,
                       String modelDir, String modelNamePrefix,
                       ResourceManager rm) throws Exception{
        super(OneMdlOrTwoMdl,saveCache,forceUpdate,lambda,maxRound,seed,modelDir,modelNamePrefix);
        initDataAndModel();
        //this.lambda = lambda;//only used in 2-model
        this.rm = rm;
        this.graphSamplingRate = graphSamplingRate;
        this.graphSamplingMode = graphSamplingMode;
        switch (this.graphSamplingMode){
            // graph sampling
            case 0:
                for(myTemporalDocument doc:trainStructs_partial)
                    doc.getGraph().downSamplingRelations(this.graphSamplingRate,this.seed++);
                break;
            // document sampling (each doc is still full)
            case 1: // #doc = #all_doc * graphSamplingRate
                ListSampler<myTemporalDocument> listSampler = new ListSampler<>(element -> false);
                trainStructs_partial = listSampler.ListSampling(trainStructs_partial,graphSamplingRate,new Random(this.seed++));
                break;
            case 2: // #edge = #all_edge * graphSamplingRate; docs are ordered
            case 3: // #edge = #all_edge * graphSamplingRate; docs are shuffled
                int totalRelCnt = 0;
                for(myTemporalDocument doc:trainStructs_partial)
                    totalRelCnt+= doc.getGraph().getAllEERelations(-1).size();
                System.out.println(myLogFormatter.fullBlockLog("Total TempRels in Partial: "+totalRelCnt));
                List<myTemporalDocument> downsampledDocs = new ArrayList<>();
                totalRelCnt *= this.graphSamplingRate;
                System.out.println(myLogFormatter.fullBlockLog("Total TempRels in Partial After Downsampling: "+totalRelCnt));
                if(this.graphSamplingMode==3){
                    Random rng = new Random(this.seed++);
                    Collections.shuffle(trainStructs_partial,rng);
                }
                for(myTemporalDocument doc:trainStructs_partial){
                    totalRelCnt-= doc.getGraph().getAllEERelations(-1).size();
                    if(totalRelCnt<=0)
                        break;
                    downsampledDocs.add(doc);
                }
                trainStructs_partial = downsampledDocs;
                break;
            default:
                System.out.println("[WARNING] Wrong graphSamplingMode.");
        }
    }

    public void ILPSetup(boolean ilp, boolean respectExistingTempRelsInCoDL, boolean respectAsHardConstraints){
        this.ilp = ilp;
        this.respectExistingTempRelsInCoDL = respectExistingTempRelsInCoDL;
        this.respectAsHardConstraints = respectAsHardConstraints;
        System.out.printf("[SETUP] ilp=%s, respectExistingTempRelsInCoDL=%s, respectAsHardConstraints=%s\n",ilp,respectExistingTempRelsInCoDL,respectAsHardConstraints);
    }

    public void loadData() throws Exception{
        myDatasetLoader myLoader = new myDatasetLoader();
        if(!debug) {
            // real data
            System.out.println(myLogFormatter.startBlockLog("Loading partial data (auto corrected)"));
            trainStructs_partial = myLoader.getTimeBank_Minus_TBDense_autoCorrected();
            //trainStructs_partial.addAll(myLoader.getAQUAINT_autoCorrected());
            System.out.println("Partial data: " + trainStructs_partial.size() + " documents.");
            System.out.println(myLogFormatter.endBlockLog("Loading partial data (auto corrected)"));

            System.out.println(myLogFormatter.startBlockLog("Loading full data (auto corrected)"));
            trainStructs_full = myLoader.getTBDense_Train_autoCorrected();
            trainStructs_full.addAll(myLoader.getTBDense_Dev_autoCorrected());
            trainStructs_full.addAll(myLoader.getTBDense_Test_autoCorrected());
            System.out.println("Full data: " + trainStructs_full.size() + " documents.");
            System.out.println(myLogFormatter.endBlockLog("Loading full data (auto corrected)"));
        }
        else{
            // mock data
            System.out.println("Loading partial data...");
            trainStructs_partial = myLoader.getTBDense_Test_autoCorrected();
            System.out.println("Loading full data...");
            trainStructs_full = myLoader.getTBDense_Dev_autoCorrected();
        }
    }
    @Override
    public void loadData_1model() throws Exception{
        loadData();
    }

    @Override
    public void loadData_2model() throws Exception{
        loadData();
    }

    @Override
    public Learner loadBaseCls() throws Exception{
        System.out.println(myLogFormatter.fullBlockLog("Retraining base classifier (incorporate partial documents in)"));
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        List<myTemporalDocument> structList4basecls = new ArrayList<>();
        structList4basecls.addAll(trainStructs_full);
        structList4basecls.addAll(trainStructs_partial);
        List<TemporalRelation_EE> atomList = new ArrayList<>();
        for(myTemporalDocument doc:structList4basecls){
            doc.extractAllFeats(rm.getInt("EVENT_TEMPREL_WINDOW"));
            atomList.addAll(doc.getGraph().getAllEERelations(-1));
        }
        return learnUtil(atomList,-1);
    }

    @Override
    public Learner loadSavedCls() throws Exception {
        String[] modelandlexpath = modelAndLexPath();
        return new eeTempRelCls(modelandlexpath[0],modelandlexpath[1]);
    }

    @Override
    public void setCacheDir() {
        String cacheDir = "serialization"+ File.separator+modelNamePrefix;
        if(respectExistingTempRelsInCoDL)
            cacheDir += respectAsHardConstraints?"hardConst":"softConst";
        super.setCacheDir(cacheDir);
    }

    @Override
    public String getStructId(myTemporalDocument st) {
        return st.getDocid();
    }


    @Override
    public myTemporalDocument inference(myTemporalDocument doc) {
        return inference(doc,respectExistingTempRelsInCoDL);
    }

    public void evalTest() throws Exception{
        myDatasetLoader myLoader = new myDatasetLoader();
        System.out.println(myLogFormatter.startBlockLog("Loading test data (auto corrected)"));
        List<myTemporalDocument> testset = myLoader.getPlatinum_autoCorrected();

        List<myTemporalDocument> testset_inf = new ArrayList<>();
        for(myTemporalDocument doc:testset){
            myTemporalDocument doc_inf = inference(doc,false);
            testset_inf.add(doc_inf);
        }
        myTemporalDocument.NaiveEvaluator(testset,testset_inf,1);
    }

    private myTemporalDocument inference(myTemporalDocument doc, boolean respectExistingTempRels) {
        myTemporalDocument doc_inf = new myTemporalDocument(doc);// deep copy
        TempRelLabelerMultiLBJ tempRelLabelerMultiLBJ = new TempRelLabelerMultiLBJ(multiClassifiers);
        TempRelAnnotator tra = new TempRelAnnotator(doc_inf,null,tempRelLabelerMultiLBJ,null,rm,ilp);
        TempRelAnnotator.performET = false;
        tra.setup(true,true,respectExistingTempRels,respectAsHardConstraints);
        tra.annotator();
        if(CoDL_PartialVsFull.debug) {
            doc.getGraph().graphVisualization("data/html/before_inf");
            doc_inf.getGraph().graphVisualization("data/html/after_inf");
        }
        return doc_inf;
    }

    public Learner learnUtil(List<TemporalRelation_EE> atomList, int currIter){
        String modelPath = String.format("%s_currIter%d.lc", modelDir+ File.separator+modelNamePrefix, currIter);
        String lexiconPath = String.format("%s_currIter%d.lex", modelDir+ File.separator+modelNamePrefix, currIter);
        eeTempRelCls classifier = new eeTempRelCls(modelPath, lexiconPath);

        ParamLBJ.EETempRelClassifierPerceptronParams.learningRate = learningRate;
        ParamLBJ.EETempRelClassifierPerceptronParams.thickness = thickness;
        ListSampler<TemporalRelation_EE> listSampler = new ListSampler<>(
                element -> !element.getLabel().equals(TemporalRelType.relTypes.VAGUE.getName())
                        &&!element.getLabel().equals(TemporalRelType.relTypes.EQUAL.getName())
        );
        Random rng = new Random(seed++);
        //sr_standard = listSampler.autoSelectSamplingRate(atomList);
        //System.out.println(myLogFormatter.fullBlockLog(String.format("Auto Selection of Sampling Rate: %.4f",sr_standard)));
        System.out.println(myLogFormatter.fullBlockLog("Don't automatically adjust any sampling rate."));
        sr_standard = 1d;
        System.out.println(myLogFormatter.fullBlockLog(String.format("Adjusted Sampling Rate: %.4f",samplingRate*sr_standard)));
        atomList = listSampler.ListSampling(atomList,samplingRate*sr_standard,rng);

        System.out.println(myLogFormatter.fullBlockLog("Learning from "+atomList.size()+" TempRels."));
        classifier.forget();
        classifier.beginTraining();
        for(int iter=0;iter<learningRound;iter++){
            Collections.shuffle(atomList, new Random(seed++));
            for(TemporalRelation_EE ee:atomList){
                try{
                    if(ee.isNull())
                        System.out.println();
                    classifier.learn(ee);
                }
                catch (Exception e){
                    System.out.println("Exception in learn().");
                    e.printStackTrace();
                }
            }
        }
        classifier.doneLearning();
        return classifier;
    }
    @Override
    public Learner learn(List<myTemporalDocument> structList, int currIter) {
        List<TemporalRelation_EE> atomList = new ArrayList<>();
        for(myTemporalDocument doc:structList){
            doc.extractAllFeats(rm.getInt("EVENT_TEMPREL_WINDOW"));
            atomList.addAll(doc.getGraph().getAllEERelations(-1));
        }
        return learnUtil(atomList,currIter);
    }

    public static void cmdParser(String[] args) {
        Options options = new Options();

        Option modelDir = new Option("d", "modelDir", true, "model output directory");
        modelDir.setRequired(true);
        options.addOption(modelDir);

        Option modelName = new Option("n", "modelName", true, "model name");
        modelName.setRequired(true);
        options.addOption(modelName);

        Option samplingRate = new Option("sr", "samplingRate", true, "temporal graph sampling rate to generate partial graphs");
        samplingRate.setRequired(true);
        options.addOption(samplingRate);

        Option samplingMode = new Option("sm", "samplingMode", true, "temporal graph sampling mode (0:graphs. 1: documents)");
        samplingMode.setRequired(true);
        options.addOption(samplingMode);

        Option seed = new Option("sd", "seed", true, "seed");
        seed.setRequired(false);
        options.addOption(seed);

        Option OneMdlOrTwoMdl = new Option("o", "OneMdlOrTwoMdl", false, "1-model or 2-model in codl");
        OneMdlOrTwoMdl.setRequired(false);
        options.addOption(OneMdlOrTwoMdl);

        Option maxIter = new Option("max", "maxIter", true, "max iteration in CoDL");
        maxIter.setRequired(false);
        options.addOption(maxIter);

        Option lambda = new Option("lambda", "lambda", true, "lambda in 2-model");
        lambda.setRequired(false);
        options.addOption(lambda);

        Option ilp = new Option("i", "ilp", false, "Use ILP or not.");
        ilp.setRequired(false);
        options.addOption(ilp);

        Option respect = new Option("r", "respect", false, "Respect existing relations in docs.");
        respect.setRequired(false);
        options.addOption(respect);

        Option hard = new Option("h", "hardConstraint", false, "Use existing relations as hard constraints. (no effect when hard=false)");
        hard.setRequired(false);
        options.addOption(hard);

        Option forceUpdate = new Option("f", "forceUpdate", false, "force update in CoDL");
        forceUpdate.setRequired(false);
        options.addOption(forceUpdate);

        Option saveCache = new Option("cache", "cache", false, "save CoDL cache or not");
        saveCache.setRequired(false);
        options.addOption(saveCache);

        Option debug = new Option("debug", "debug", false, "debug (load fewer docs)");
        debug.setRequired(false);
        options.addOption(debug);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("TemprelPerceptronTrainer_EE", options);

            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception{
        cmdParser(args);
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        int window = rm.getInt("EVENT_TEMPREL_WINDOW");

        String modelDir = cmd.getOptionValue("modelDir");
        String modelPrefixName = cmd.getOptionValue("modelName");
        double samplingRate = Double.valueOf(cmd.getOptionValue("samplingRate"));
        int samplingMode = Integer.valueOf(cmd.getOptionValue("samplingMode"));
        int seed = Integer.valueOf(cmd.getOptionValue("seed","0"));
        int maxIter = Integer.valueOf(cmd.getOptionValue("max","1"));

        boolean OneMdlOrTwoMdl = cmd.hasOption("OneMdlOrTwoMdl");
        double lambda = Double.valueOf(cmd.getOptionValue("lambda","0.1"));
        boolean ilp = cmd.hasOption("ilp");
        boolean respect = cmd.hasOption("respect");
        boolean hard = cmd.hasOption("hardConstraint");
        modelPrefixName += String.format("_sr%.2f_win%d_%s_%s_%s",samplingRate,window,OneMdlOrTwoMdl?"1mdl":"2mdl",ilp?"global":"local",respect?"respect":"norespect");
        if(respect)
            modelPrefixName += hard?"_hardConst":"_softConst";
        if(samplingMode>0)
            modelPrefixName += "_sm"+samplingMode;
        if(seed>0)
            modelPrefixName += "_sd"+seed;
        if(!OneMdlOrTwoMdl)
            modelPrefixName += "_lambda"+lambda;
        CoDL_PartialVsFull.debug = cmd.hasOption("debug");
        boolean forceUpdate = cmd.hasOption("forceUpdate") || CoDL_PartialVsFull.debug;
        boolean saveCache = cmd.hasOption("cache");

        if(CoDL_PartialVsFull.debug)
            modelPrefixName += "_debug";

        CoDL_PartialVsFull tester = new CoDL_PartialVsFull(maxIter,seed,samplingRate,samplingMode,OneMdlOrTwoMdl,lambda,forceUpdate,saveCache,modelDir,modelPrefixName,rm);
        tester.ILPSetup(ilp,respect,hard);
        System.out.println("Running CoDL...");
        tester.CoDL();
        if(!CoDL_PartialVsFull.debug) {
            tester.saveClassifiers();
            tester.evalTest();
        }
        else // debug
            tester.evalTest();
    }
}