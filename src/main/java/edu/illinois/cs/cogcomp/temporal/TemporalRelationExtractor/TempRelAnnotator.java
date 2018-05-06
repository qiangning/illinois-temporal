package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.Triplet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelationType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.eventDetector;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.normalizer.main.TemporalChunkerAnnotator;
import edu.illinois.cs.cogcomp.temporal.normalizer.main.TemporalChunkerConfigurator;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.EventNodeType;
import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.TimexNodeType;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.*;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class TempRelAnnotator {
    private myTemporalDocument doc;
    private EventAxisLabeler axisLabeler;
    private TempRelLabeler tempRelLabeler;
    private TemporalChunkerAnnotator tca;
    private ResourceManager rm;

    private boolean ilp;
    private int n_entitiy;
    // all first two dimensions are upper triangle
    private double[][][] local_score;//n_entitiy x n_entitiy x n_relation
    private boolean[][] ignoreMap;//n_entitiy x n_entitiy
    private List<int[][][]> constraintMap;// A list of {n_entitiy x n_entitiy x (n_relation+1)} //+1dim is the "1" in x1+x2+x3=1
    private static List<Triplet<Integer,Integer,List<Integer>>> transitivityMap;
    public int[][] result;

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
        this(doc,defaultAxisLabeler(),defaultTempRelLabeler(),rm);
    }

    public TempRelAnnotator(myTemporalDocument doc, EventAxisLabeler axisLabeler, TempRelLabeler tempRelLabeler, ResourceManager rm) {
        this(doc,axisLabeler,tempRelLabeler,rm,true);
    }

    public TempRelAnnotator(myTemporalDocument doc, EventAxisLabeler axisLabeler, TempRelLabeler tempRelLabeler, ResourceManager rm, boolean ilp) {
        this.doc = doc;
        this.axisLabeler = axisLabeler;
        this.tempRelLabeler = tempRelLabeler;
        this.rm = rm;
        this.ilp = ilp;
        tca = defaultTemporalChunkerAnnotator();
        if(doc.getDct()!=null)
            setDCT(doc.getDct().getNormVal());
    }

    private void initAllArrays4ILP(){
        n_entitiy = doc.getEventList().size();
        local_score = new double[n_entitiy][n_entitiy][TemporalRelType.relTypes.values().length];
        ignoreMap = new boolean[n_entitiy][n_entitiy];
        constraintMap = new ArrayList<>();
        int[][][] uniqueness = new int[n_entitiy][n_entitiy][TemporalRelType.relTypes.values().length+1];
        for(int i=0;i<n_entitiy;i++){
            Arrays.fill(ignoreMap[i],true);
            for(int j=0;j<n_entitiy;j++) {
                Arrays.fill(local_score[i][j], 0);
                Arrays.fill(uniqueness[i][j],1);
            }
        }
        constraintMap.add(uniqueness);

        if(transitivityMap==null|| transitivityMap.size()==0){
            transitivityMap = new ArrayList<>();
            TemporalRelType.relTypes[] reltypes = TemporalRelType.relTypes.values();
            int n = reltypes.length;
            for(int i=0;i<n;i++){
                TemporalRelType.relTypes r1 = reltypes[i];
                for(int j=0;j<n;j++){
                    TemporalRelType.relTypes r2 = reltypes[j];
                    //    public List<BinaryRelationType> transitivity(BinaryRelationType rel1, BinaryRelationType rel2)
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
        tca.addDocumentCreationTime(dct);
    }

    public void annotator(){
        annotator(false,false);
    }

    public void annotator(boolean goldEvent, boolean goldTimex){
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
        tempRelAnnotator();
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

    public void tempRelAnnotator(){
        int window = rm.getInt("EVENT_TEMPREL_WINDOW");
        List<EventTemporalNode> eventList = doc.getEventList();

        // extract features
        for(EventTemporalNode e:eventList){
            e.extractPosLemmaWin(window);
            e.extractSynsets();
        }

        for(EventTemporalNode e1:eventList){
            int i = eventList.indexOf(e1);
            for(EventTemporalNode e2:eventList){
                int j = eventList.indexOf(e2);
                if(e1.isEqual(e2)||e1.getTokenId()>e2.getTokenId())
                    continue;
                TemporalRelation_EE ee = new TemporalRelation_EE(e1,e2,new TemporalRelType(TemporalRelType.relTypes.VAGUE),doc);

                // extract features
                ee.extractAllFeats();

                TemporalRelType reltype = tempRelLabeler.tempRelLabel(ee);
                if(reltype.isNull())
                    continue;
                ee.setRelType(reltype);
                ignoreMap[i][j] = false;
                local_score[i][j] = ee.getRelType().getScores();
                doc.getGraph().addRelNoDup(ee);
            }
        }
        if(ilp) {
            TempRelInferenceWrapper solver = new TempRelInferenceWrapper(n_entitiy, TemporalRelType.relTypes.values().length,
                    local_score, ignoreMap, constraintMap, transitivityMap);
            solver.solve();
            result = solver.getResult();
            for (int i = 0; i < n_entitiy; i++) {
                for (int j = i + 1; j < n_entitiy; j++) {
                    if (ignoreMap[i][j])
                        continue;
                    TemporalRelType reltype = new TemporalRelType(TemporalRelType.relTypes.getRelTypesFromIndex(result[i][j]));
                    TemporalRelation tlink = doc.getGraph().getRelBetweenNodes(doc.getEventList().get(i).getUniqueId(), doc.getEventList().get(j).getUniqueId());
                    if (tlink != null)
                        tlink.setRelType(reltype);
                }
            }
        }
    }

    public void setAxisLabeler(EventAxisLabeler axisLabeler) {
        this.axisLabeler = axisLabeler;
    }

    public void setTempRelLabeler(TempRelLabeler tempRelLabeler) {
        this.tempRelLabeler = tempRelLabeler;
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

    private static TempRelLabeler defaultTempRelLabeler(){
        String temprelMdlDir = "models/tempRel", temprelMldNamePrefix = "eeTempRelCls";//eeTempRelCls_sent0_labelMode0_clsMode0_win3
        eeTempRelCls cls0 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+"_labelMode0_clsMode0_win3.lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+"_labelMode0_clsMode0_win3.lex");
        eeTempRelCls cls1 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+"_labelMode0_clsMode0_win3.lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+"_labelMode0_clsMode0_win3.lex");
        return new TempRelLabelerLBJ(cls0,cls1);

        /*String temprelMdlDir = "models", temprelMldNamePrefix = "eeTempRelCls";
        eeTempRelCls cls_mod1_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,0));
        eeTempRelCls cls_mod2_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,0));
        eeTempRelCls cls_mod1_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,1));
        eeTempRelCls cls_mod2_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,1));
        TempRelLabelerLBJ tempRelLabelerLBJ = new TempRelLabelerLBJ(cls_mod1_dist0,cls_mod2_dist0,cls_mod1_dist1,cls_mod2_dist1);*/
    }

    public static void rawtext2graph() throws Exception{
        String text = "He fell in love with her after they first met 9 years ago. Now they are expecting their first baby this June.";
        myTemporalDocument doc = new myTemporalDocument(text,"test","2010-05-04");
        TempRelAnnotator tra = new TempRelAnnotator(doc);
        tra.annotator();
        System.out.println();
    }

    public static void main(String[] args) throws Exception{
        boolean goldEvent = true, goldTimex = true;
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        List<TemporalDocument> allDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
        HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
        HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));

        EventAxisLabeler eventAxisLabeler = defaultAxisLabeler();
        TempRelLabeler tempRelLabeler = defaultTempRelLabeler();
        List<myTemporalDocument> myAllDocs = new ArrayList<>(), myAllDocs_Gold = new ArrayList<>();
        int cnt=0;
        for(TemporalDocument d:allDocs){
            String docid = d.getDocID();
            if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                continue;
            myTemporalDocument doc = new myTemporalDocument(d,1);
            doc.keepAnchorableEvents(axisMap.get(doc.getDocid()));

            myTemporalDocument docGold = new myTemporalDocument(d,1);
            docGold.keepAnchorableEvents(axisMap.get(doc.getDocid()));
            docGold.loadRelationsFromMap(relMap.get(doc.getDocid()),0);

            myAllDocs.add(doc);
            myAllDocs_Gold.add(docGold);

            TempRelAnnotator tra = new TempRelAnnotator(doc,eventAxisLabeler,tempRelLabeler,rm);
            tra.annotator(goldEvent,goldTimex);
            cnt++;
            if(cnt>=20)
                break;
        }

        myTemporalDocument.NaiveEvaluator(myAllDocs_Gold,myAllDocs,1);
    }
}
