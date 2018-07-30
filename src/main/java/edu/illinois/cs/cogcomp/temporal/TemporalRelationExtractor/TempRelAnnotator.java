package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.Triplet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelationType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.eventDetector;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls_ET.etTempRelCls;
import edu.illinois.cs.cogcomp.temporal.normalizer.main.TemporalChunkerAnnotator;
import edu.illinois.cs.cogcomp.temporal.normalizer.main.TemporalChunkerConfigurator;
import edu.illinois.cs.cogcomp.temporal.readers.myDatasetLoader;
import edu.illinois.cs.cogcomp.temporal.utils.myLogFormatter;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.EventNodeType;
import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.TimexNodeType;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_ON_MAIN_AXIS;

public class TempRelAnnotator {
    private myTemporalDocument doc;
    private EventAxisLabeler axisLabeler;
    private TempRelLabeler eeTempRelLabeler, etTempRelLabeler;
    private TemporalChunkerAnnotator tca;
    private ResourceManager rm;
    private boolean goldTimex=false, goldEvent=false, respectExistingTempRels=false, respectAsHardConstraints=false;

    private TempRelInferenceWrapper solver;
    private boolean ilp;
    private int n_entity;
    // all first two dimensions are upper triangle
    private double[][][] local_score;//n_entity x n_entity x n_relation
    private boolean[][] ignoreMap;//n_entity x n_entity
    private List<int[][][]> constraintMap;// A list of {n_entity x n_entity x (n_relation+1)} //+1dim is the "1" in x1+x2+x3=1
    private static List<Triplet<Integer,Integer,List<Integer>>> transitivityMap;
    private static TempRelLabeler defaultEE,defaultET;
    public int[][] result;

    public static boolean performET = true;
    public static boolean printILP = false;

    /*Constructors*/
    public TempRelAnnotator(myTemporalDocument doc){
        this(doc,null);
        try{
            rm = new temporalConfigurator().getConfig("config/directory.properties");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Default ResourceManager Loading Error. Exiting now.");
            System.exit(-1);
        }
    }

    public TempRelAnnotator(myTemporalDocument doc, ResourceManager rm) {
        this(doc,defaultAxisLabeler(), defaultTempRelLabeler_EE(),rm);
    }

    public TempRelAnnotator(myTemporalDocument doc, EventAxisLabeler axisLabeler, TempRelLabeler eeTempRelLabeler, ResourceManager rm) {
        this(doc,axisLabeler, eeTempRelLabeler, defaultTempRelLabeler_ET(), rm,true);
    }

    public TempRelAnnotator(myTemporalDocument doc, EventAxisLabeler axisLabeler, TempRelLabeler eeTempRelLabeler, TempRelLabeler etTempRelLabeler, ResourceManager rm, boolean ilp) {
        this.doc = doc;
        this.axisLabeler = axisLabeler;
        this.eeTempRelLabeler = eeTempRelLabeler;
        this.etTempRelLabeler = etTempRelLabeler;
        this.rm = rm;
        this.ilp = ilp;
        tca = defaultTemporalChunkerAnnotator();
        if(doc.getDct()!=null)
            setDCT(doc.getDct().getNormVal());
    }

    /*Functions*/

    private void initAllArrays4ILP(){
        n_entity = doc.getEventList().size();
        local_score = new double[n_entity][n_entity][TemporalRelType.relTypes.values().length];
        ignoreMap = new boolean[n_entity][n_entity];
        constraintMap = new ArrayList<>();
        int[][][] uniqueness = new int[n_entity][n_entity][TemporalRelType.relTypes.values().length+1];
        for(int i = 0; i< n_entity; i++){
            Arrays.fill(ignoreMap[i],true);
            for(int j = 0; j< n_entity; j++) {
                Arrays.fill(local_score[i][j], 0);
                Arrays.fill(uniqueness[i][j],1);
            }
        }
        constraintMap.add(uniqueness);
        int[][][] AllNonNull = new int[n_entity][n_entity][TemporalRelType.relTypes.values().length+1];
        for(int i = 0; i< n_entity; i++){
            for(int j = 0; j< n_entity; j++){
                AllNonNull[i][j][TemporalRelType.relTypes.NULL.getIndex()] = 1;
                AllNonNull[i][j][TemporalRelType.relTypes.values().length] = 0;
            }
        }
        constraintMap.add(AllNonNull);

        if(transitivityMap==null|| transitivityMap.size()==0){
            transitivityMap = new ArrayList<>();
            TemporalRelType.relTypes[] reltypes = TemporalRelType.relTypes.values();
            int n = reltypes.length;
            for(int i=0;i<n;i++){
                TemporalRelType.relTypes r1 = reltypes[i];
                for(int j=0;j<n;j++){
                    TemporalRelType.relTypes r2 = reltypes[j];
                    List<BinaryRelationType> trans = new TemporalRelType(r1).transitivity(new TemporalRelType(r2));
                    List<Integer> trans_ids = new ArrayList<>();
                    for(BinaryRelationType brt:trans){
                        trans_ids.add(((TemporalRelType) brt).getReltype().getIndex());
                    }
                    transitivityMap.add(new Triplet<>(r1.getIndex(),r2.getIndex(),trans_ids));
                }
            }
        }
    }

    public void setDCT(String dct){
        System.out.println(dct);
        tca.addDocumentCreationTime(dct);
    }

    public void setup(boolean goldTimex, boolean goldEvent, boolean respectExistingTempRels, boolean respectAsHardConstraints){
        //System.out.printf("[TempRelAnnotator] Setup: goldTimex=%s, goldEvent=%s, respectExistingTempRels=%s, respectAsHardConstraints=%s\n",goldTimex,goldEvent,respectExistingTempRels,respectAsHardConstraints);
        this.goldEvent = goldEvent;
        this.goldTimex = goldTimex;
        this.respectExistingTempRels = respectExistingTempRels;
        this.respectAsHardConstraints = respectAsHardConstraints;
    }

    public void annotator(){
        if(!goldEvent){
            doc.dropAllEventNodes();
            axisAnnotator();
        }
        initAllArrays4ILP();
        if(!goldTimex) {
            doc.dropAllTimexNodes();
            try {
                timexAnnotator();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(!respectExistingTempRels) {
            //System.out.println("[TempRelAnnotator.annotator] Start from scratch. Drop all relations in "+doc.getDocid());
            doc.dropAllRelations();
        }
        doc.addTTRelationsBasedOnNormVals();
        if(performET) {
            etTempRelAnnotator();
        }
        doc.addEERelationsBasedOnETAndTT();
        eeTempRelAnnotator();
    }

    public void axisAnnotator(){
        int window = rm.getInt("EVENT_DETECTOR_WINDOW");
        TextAnnotation ta = doc.getTextAnnotation();
        int eiid = 0;
        List<EventTokenCandidate> eventTokenCandidateList = doc.generateAllEventTokenCandidates(window,new HashMap<>());
        EventTokenCandidate prev_event = null;
        for(EventTokenCandidate etc:eventTokenCandidateList){
            etc.setPrev_event(prev_event);
            String label = axisLabeler.axisLabel(etc);
            etc.setLabel(label);
            if(!label.equals(LABEL_NOT_ON_ANY_AXIS)) {
                prev_event = etc;
            }
            if(label.toLowerCase().equals(LABEL_ON_MAIN_AXIS)){
                EventTemporalNode tmpNode = new EventTemporalNode(eiid, EventNodeType,ta.getToken(etc.getTokenId()), eiid,eiid,eiid, etc.getTokenId(),ta,doc);
                doc.addEvent(tmpNode);
                eiid++;
            }
        }
    }

    public void timexAnnotator() throws Exception{
        if(doc.getDct()!=null
                &&doc.getGraph().getNode(doc.getDct().getUniqueId())==null)// if dct isn't in graph yet
            doc.addTimex(doc.getDct());
        TextAnnotation ta = doc.getTextAnnotation();
        tca.addView(ta);
        View temporalViews = ta.getView(ViewNames.TIMEX3);
        List<Constituent> constituents = temporalViews.getConstituents();
        for(int i=0;i<constituents.size();i++){
            Constituent c = constituents.get(i);
            int sentId = ta.getSentenceId(c.getStartSpan());
            doc.addTimex(new TimexTemporalNode(doc.getTimexList().size(),TimexNodeType,c.toString(),doc.getTimexList().size(),c.getSpan(),sentId,false,c.getAttribute("type"),"",c.getAttribute("value"),ta));
        }
    }

    public void eeTempRelAnnotator(){// always respect existing relations in doc, so if you don't want to respect them, drop them beforehand or set respectExistingTempRels=false
        int window = rm.getInt("EVENT_TEMPREL_WINDOW");
        List<EventTemporalNode> eventList = doc.getEventList();

        // extract features
        for(EventTemporalNode e:eventList)
            e.extractAllFeats(window);

        for(EventTemporalNode e1:eventList){
            int i = eventList.indexOf(e1);
            for(EventTemporalNode e2:eventList){
                int j = eventList.indexOf(e2);
                if(e1.isEqual(e2)||e1.getTokenId()>e2.getTokenId())
                    continue;
                TemporalRelation_EE ee = doc.getGraph().getEERelBetweenEvents(e1.getUniqueId(),e2.getUniqueId());
                if(ee==null){
                    ee = new TemporalRelation_EE(e1, e2, new TemporalRelType(TemporalRelType.relTypes.NULL), doc);
                    ignoreMap[i][j] = eeTempRelLabeler.isIgnore(ee);
                }
                else{
                    ignoreMap[i][j] = false;
                }
                if(ignoreMap[i][j])
                    continue;
                ee.extractAllFeats();
                TemporalRelType reltype = ee.getRelType();
                if(reltype.isNull()){
                    reltype = eeTempRelLabeler.tempRelLabel(ee);
                    if(reltype.isNull()) {// even if eeTempRelLabeler.isIgnore(ee)==false, reltype can still be null (it's an exception when classifiers are null)
                        System.out.println("[WARNING] reltype by TempRelLabeler is unexpectedly null.");
                        continue;
                    }
                    ee.setRelType(reltype);
                    if(!ilp||!respectAsHardConstraints)
                        doc.getGraph().addRelNoDup(ee);
                }
                local_score[i][j] = reltype.getScores();
            }
        }
        // tune local scores by TT links
        // assume event time can be approx. by its closest timex
        double pseudoTT = 0.1d;
        for(TemporalRelation_TT tt:doc.getGraph().getAllTTRelations(-1)){
            TimexTemporalNode t1 = tt.getSourceNode(), t2 = tt.getTargetNode();
            if(t1.getSentId()==t2.getSentId()) continue;
            for(EventTemporalNode e1:eventList) {
                if(e1.getSentId()!=t1.getSentId()) continue;
                int i = eventList.indexOf(e1);
                for (EventTemporalNode e2 : eventList) {
                    if(e2.getSentId()!=t2.getSentId()) continue;
                    int j = eventList.indexOf(e2);
                    local_score[i][j][tt.getRelType().getReltype().getIndex()] += pseudoTT;
                }
            }
        }
        if(ilp) {
            if(respectAsHardConstraints) {
                int[][][] originalTempRelConstraintsMap = new int[n_entity][n_entity][TemporalRelType.relTypes.values().length + 1];
                boolean updated = false;
                for (int i = 0; i < n_entity; i++) {
                    for (int j = i + 1; j < n_entity; j++) {
                        Arrays.fill(originalTempRelConstraintsMap[i][j], 1);
                        TemporalRelation_EE tlink = doc.getGraph().getEERelBetweenEvents(doc.getEventList().get(i).getUniqueId(), doc.getEventList().get(j).getUniqueId());
                        if (tlink != null && !tlink.isNull()) {
                            for (int k = 0; k < TemporalRelType.relTypes.values().length; k++) {
                                if (tlink.getRelType().getReltype() == TemporalRelType.relTypes.getRelTypesFromIndex(k))
                                    continue;
                                originalTempRelConstraintsMap[i][j][k] = 0;
                            }
                            updated = true;
                        }
                    }
                }
                if (updated)
                    constraintMap.add(originalTempRelConstraintsMap);
            }
            solver = new TempRelInferenceWrapper(n_entity, TemporalRelType.relTypes.values().length,
                    local_score, ignoreMap, constraintMap, transitivityMap);
            solver.solve();
            if(respectAsHardConstraints&&solver.getSolver().unsat()){
                System.out.printf("Solving Doc %s with constraints failed. Switching to soft constraints...\n",doc.getDocid());
                constraintMap.remove(constraintMap.size()-1);
                solver = new TempRelInferenceWrapper(n_entity, TemporalRelType.relTypes.values().length,
                        local_score, ignoreMap, constraintMap, transitivityMap);
                solver.solve();
            }
            if(!solver.getSolver().isSolved()) {
                System.out.println("[TempRelAnnotator] ILP failed. Returning.");
                return;
            }
            result = solver.getResult();
            for (int i = 0; i < n_entity; i++) {
                for (int j = i + 1; j < n_entity; j++) {
                    if (ignoreMap[i][j])
                        continue;
                    TemporalRelType reltype = new TemporalRelType(TemporalRelType.relTypes.getRelTypesFromIndex(result[i][j]));
                    doc.getGraph().setRelBetweenNodes(doc.getEventList().get(i).getUniqueId(), doc.getEventList().get(j).getUniqueId(),reltype);
                }
            }

            //todo polish this snippet
            if(printILP){
                String printILP_dir = "data/ILP";
                try {
                    PrintStream ps = new PrintStream(new File(printILP_dir + File.separator + doc.getDocid()));
                    StringBuffer sb = new StringBuffer();
                    solver.getSolver().write(sb);
                    ps.println(sb.toString());
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        //doc.extractAllFeats(window);
    }

    public void etTempRelAnnotator(){
        int window = rm.getInt("EVENT_TIMEX_TEMPREL_WINDOW");
        List<EventTemporalNode> eventList = doc.getEventList();
        List<TimexTemporalNode> timexList = doc.getTimexList();

        // extract features
        for(EventTemporalNode e:eventList)
            e.extractAllFeats(window);
        for(TimexTemporalNode t:timexList)
            t.extractPosLemmaWin(window);

        // guaranteed: only one timex can be associated with a single event
        for(EventTemporalNode e:eventList){
            List<TemporalRelation_ET> et2add = new ArrayList<>();
            for(TimexTemporalNode t:timexList){
                TemporalRelation_ET et = doc.getGraph().getETRelBetweenEventTimex(e.getUniqueId(),t.getUniqueId());
                if(et==null){
                    et = new TemporalRelation_ET(e, t, new TemporalRelType(TemporalRelType.relTypes.NULL), doc);
                }
                if(etTempRelLabeler.isIgnore(et))
                    continue;
                et.extractAllFeats();
                TemporalRelType reltype = et.getRelType();
                if(reltype.isNull()){
                    reltype = etTempRelLabeler.tempRelLabel(et);
                    if(reltype.isNull()) {// even if eeTempRelLabeler.isIgnore(ee)==false, reltype can still be null (it's an exception when classifiers are null)
                        System.out.println("[WARNING] reltype by TempRelLabeler is unexpectedly null.");
                        continue;
                    }
                    et.setRelType(reltype);
                    et2add.add(et);
                }
            }
            TemporalRelation_ET bestET = null;
            double maxScore = -1d;
            for(TemporalRelation_ET et:et2add) {
                double currScore = et.getRelType().getScores()[TemporalRelType.relTypes.EQUAL.getIndex()];
                if(maxScore<currScore){
                    maxScore = currScore;
                    bestET = et;
                }
            }
            doc.getGraph().addRelNoDup(bestET);
        }
    }

    /*Getters and Setters*/

    public void setAxisLabeler(EventAxisLabeler axisLabeler) {
        this.axisLabeler = axisLabeler;
    }

    public void setEeTempRelLabeler(TempRelLabeler eeTempRelLabeler) {
        this.eeTempRelLabeler = eeTempRelLabeler;
    }

    private static EventAxisLabeler defaultAxisLabeler(){
        String axisMdlDir = "models/eventDetector", axisMdlName = "eventPerceptronDetector_win2_cls0";
        return new EventAxisLabelerLBJ(
                new eventDetector(axisMdlDir+ File.separator+axisMdlName+".lc",
                        axisMdlDir+File.separator+axisMdlName+".lex"));
    }

    private static TemporalChunkerAnnotator defaultTemporalChunkerAnnotator(){
        Properties rmProps = new TemporalChunkerConfigurator().getDefaultConfig().getProperties();
        rmProps.setProperty("useHeidelTime", "False");
        return new TemporalChunkerAnnotator(new ResourceManager(rmProps));
    }

    private static TempRelLabeler defaultTempRelLabeler_EE(){
        if(defaultEE==null) {
        /*String temprelMdlDir = "models/tempRel", temprelMldNamePrefix = "eeTempRelCls";//eeTempRelCls_sent0_labelMode0_clsMode0_win3
        eeTempRelCls cls0 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+"_labelMode0_clsMode0_win3.lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+"_labelMode0_clsMode0_win3.lex");
        eeTempRelCls cls1 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+"_labelMode0_clsMode0_win3.lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+"_labelMode0_clsMode0_win3.lex");
        return new TempRelLabelerLBJ_EE(cls0,cls1);*/

            String temprelMdlDir = "models/tempRel/bugfix", temprelMldNamePrefix = "eeTempRelClsBugFix";//eeTempRelCls_sent0_labelMode0_clsMode0_win3
            eeTempRelCls cls0 = new eeTempRelCls(temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 0 + "_labelMode0_clsMode0_win3.lc",
                    temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 0 + "_labelMode0_clsMode0_win3.lex");
            eeTempRelCls cls1 = new eeTempRelCls(temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 1 + "_labelMode0_clsMode0_win3.lc",
                    temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 1 + "_labelMode0_clsMode0_win3.lex");
            defaultEE = new TempRelLabelerLBJ_EE(cls0, cls1);
        }
        return defaultEE;

        /*String temprelMdlDir = "models", temprelMldNamePrefix = "eeTempRelCls";
        eeTempRelCls cls_mod1_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,0));
        eeTempRelCls cls_mod2_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,0));
        eeTempRelCls cls_mod1_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,1));
        eeTempRelCls cls_mod2_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,1));
        TempRelLabelerLBJ_EE tempRelLabelerLBJ = new TempRelLabelerLBJ_EE(cls_mod1_dist0,cls_mod2_dist0,cls_mod1_dist1,cls_mod2_dist1);*/
    }

    public static TempRelLabeler defaultTempRelLabeler_ET(){
        if(defaultET==null) {
            String temprelMdlDir = "models/tempRel_ET", temprelMldNamePrefix = "etTempRelCls";//eeTempRelCls_sent0_labelMode0_clsMode0_win3
            etTempRelCls cls0 = new etTempRelCls(temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 0 + "_labelMode0_clsMode0_win3.lc",
                    temprelMdlDir + File.separator + temprelMldNamePrefix + "_sent" + 0 + "_labelMode0_clsMode0_win3.lex");
            defaultET = new TempRelLabelerLBJ_ET(cls0);
        }
        return defaultET;
    }

    public static void rawtext2graph(String dir, String fname) throws Exception{
        String text = "The last surviving member of the team which first conquered Everest in 1953 has died in a Derbyshire nursing home. George Lowe, 89, died in Ripley on Wednesday after a long-term illness, with his wife Mary by his side. New Zealand-born Mr Lowe was part of the team that helped Sir Edmund Hillary and Tenzing Norgay to the summit in 1953.Family friend and historian Dr Huw Lewis-Jones paid tribute to a \"gentle soul and fine climber\" who shunned the limelight. Mr Lowe also took part in the trans-Antarctic expedition of 1957-58, which made the first successful overland crossing of Antarctica via the South Pole. He later made expeditions to Greenland, Greece and Ethiopia. Speaking to the BBC in 1995, Mr Lowe said of his Antarctic adventure: \"We estimated we could do it in 100 days, and we got across on the 99th day. \"There was a great feeling of euphoria from everyone. It had a multiplying effect. \"We were pleased that England and New Zealand knew about it, and we thought that's where it would stop.\" He also talked about his \"second job\" as the group's cameraman, and having to wear four pairs of gloves to work the clockwork camera. \"When there were dramas, there was a split problem. Do you take part in the urgency - or do you record it?\" he said. Dr Lewis-Jones, the former curator at the Scott Polar Research Institute at the University of Cambridge, who first met Mr Lowe in 2005, called him a \"hero\". \"I don't often use that word but then it is not very often that you get to meet one,\" he said. A book of memoirs and photographs from the climb by Mr Lowe, which he worked on with Dr Lewis-Jones, is due to be published in May. He said: \"Lowe was a brilliant, kind fellow who never sought the limelight... and 60 years on from Everest his achievements deserve wider recognition. \"He was involved in two of the most important explorations of the 20th Century... yet remained a humble, happy man right to the end... an inspirational lesson to us all.\" Before retiring in 1984, Mr Lowe worked as an Inspector of Schools with the Department of Education and Sciences, and he leaves three sons from a previous marriage. The last British climbing member of the 1953 team, Mike Westmacott, died last June.";
        String dct = "2013-03-22";
        myTemporalDocument doc = new myTemporalDocument(text,"test_longDist",dct);
        TempRelAnnotator tra = new TempRelAnnotator(doc);
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        tra.annotator();
        timer.end();
        doc.getGraph().reduction();
        doc.getGraph().graphVisualization("data/html");
        doc.getGraph().chainVisualization("data/html");
        System.out.println(timer.getTimeSeconds()+" seconds.");
    }

    public static String processRawText(String text, String dct) throws Exception{
        System.out.println(text);
        myTemporalDocument doc = new myTemporalDocument(text, "test", dct);
        TempRelAnnotator tra = new TempRelAnnotator(doc);
        tra.annotator();
        doc.getGraph().reduction();
        String graphText = doc.getGraph().graphVisualization("");
        String chainText = doc.getGraph().chainVisualization("");
        String originalText = doc.taVisualization();
        return graphText + "SPRTTRPS" + chainText + "SPRTTRPS" + originalText;
    }

    public static void main(String[] args) throws Exception{
        rawtext2graph("data/SampleInput","apple");
        /*myDatasetLoader loader = new myDatasetLoader();
        boolean goldEvent = false, goldTimex = false;
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");

        EventAxisLabeler eventAxisLabeler = defaultAxisLabeler();
        TempRelLabeler eeTempRelLabeler = defaultTempRelLabeler_EE();
        TempRelLabeler etTempRelLabeler = defaultTempRelLabeler_ET();
        List<myTemporalDocument> myAllDocs = loader.getPlatinum_autoCorrected(), myAllDocs_Gold = loader.getPlatinum_autoCorrected();
        TempRelAnnotator.performET = true;
        for(myTemporalDocument doc:myAllDocs){
            TempRelAnnotator tra = new TempRelAnnotator(doc,eventAxisLabeler,eeTempRelLabeler,etTempRelLabeler,rm,true);
            tra.setup(goldTimex,goldEvent,false,false);
            tra.annotator();
        }

        myTemporalDocument.NaiveEvaluator(myAllDocs_Gold,myAllDocs,1);
        myTemporalDocument.AwarenessEvaluator(myAllDocs_Gold,myAllDocs,1);*/
    }
}
