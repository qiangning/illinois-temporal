package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.Triplet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelationType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.eventDetector;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls2;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import util.TempLangMdl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.EventNodeType;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_ON_MAIN_AXIS;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class TempRelAnnotator {
    private myTemporalDocument doc;
    private EventAxisLabeler axisLabeler;
    private TempRelLabeler tempRelLabeler;
    private ResourceManager rm;
    private WNSim wnsim;

    private int n_entitiy;
    // all first two dimensions are upper triangle
    private double[][][] local_score;//n_entitiy x n_entitiy x n_relation
    private boolean[][] ignoreMap;//n_entitiy x n_entitiy
    private List<int[][][]> constraintMap;// A list of {n_entitiy x n_entitiy x (n_relation+1)} //+1dim is the "1" in x1+x2+x3=1
    private static List<Triplet<Integer,Integer,List<Integer>>> transitivityMap;
    public int[][] result;

    public TempRelAnnotator(myTemporalDocument doc, EventAxisLabeler axisLabeler, TempRelLabeler tempRelLabeler, ResourceManager rm,WNSim wnsim) {
        this.doc = doc;
        this.axisLabeler = axisLabeler;
        this.tempRelLabeler = tempRelLabeler;
        this.rm = rm;
        this.wnsim = wnsim;
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

    public void axisAnnotator(){
        int window = rm.getInt("EVENT_DETECTOR_WINDOW");
        doc.dropAllEventsAndTimexes();
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
                EventTemporalNode tmpNode = new EventTemporalNode(eiid, EventNodeType,ta.getToken(etc.getTokenId()), eiid,eiid,eiid, etc.getTokenId(),ta);
                doc.addEvent(tmpNode);
                eiid++;
            }
        }
        initAllArrays4ILP();
    }

    public void tempRelAnnotator(boolean ilp,HashMap<String,HashMap<String,HashMap<TLINK.TlinkType,Integer>>> tempLangMdl){
        int window = rm.getInt("EVENT_TEMPREL_WINDOW");
        List<EventTemporalNode> eventList = doc.getEventList();

        // extract features
        for(EventTemporalNode e:eventList){
            e.extractPosLemmaWin(window);
            e.extractSynsets(wnsim);
        }

        for(EventTemporalNode e1:eventList){
            int i = eventList.indexOf(e1);
            for(EventTemporalNode e2:eventList){
                int j = eventList.indexOf(e2);
                if(e1.isEqual(e2)||e1.getTokenId()>e2.getTokenId())
                    continue;
                TemporalRelation_EE ee = new TemporalRelation_EE(e1,e2,new TemporalRelType(TemporalRelType.relTypes.VAGUE));

                // extract features
                ee.extractSignalWords();
                ee.readCorpusStats(tempLangMdl);

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

    public static void main(String[] args) throws Exception{
        boolean ILP = true;
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        WNSim wnsim = WNSim.getInstance(rm.getString("WordNet_Dir"));
        List<TemporalDocument> allDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
        HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
        HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));

        String lm_path = rm.getString("TemProb_Dir");
        TempLangMdl tempLangMdl = TempLangMdl.getInstance(lm_path);

        String axisMdlDir = "models/eventDetector", axisMdlName = "eventPerceptronDetector_win2_cls0";
        EventAxisLabelerLBJ axisLabelerLBJ = new EventAxisLabelerLBJ(
                new eventDetector(axisMdlDir+ File.separator+axisMdlName+".lc",
                        axisMdlDir+File.separator+axisMdlName+".lex"));
        String temprelMdlDir = "models/tempRel/old", temprelMldNamePrefix = "eeTempRelCls_temprob_mod0_win2";
        eeTempRelCls2 cls0 = new eeTempRelCls2(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+".lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+".lex");
        eeTempRelCls2 cls1 = new eeTempRelCls2(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+".lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+".lex");
        TempRelLabelerLBJ tempRelLabelerLBJ = new TempRelLabelerLBJ(cls0,cls1);

        /*String temprelMdlDir = "models", temprelMldNamePrefix = "eeTempRelCls";
        eeTempRelCls cls_mod1_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,0));
        eeTempRelCls cls_mod2_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,0));
        eeTempRelCls cls_mod1_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,1));
        eeTempRelCls cls_mod2_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,1));
        TempRelLabelerLBJ tempRelLabelerLBJ = new TempRelLabelerLBJ(cls_mod1_dist0,cls_mod2_dist0,cls_mod1_dist1,cls_mod2_dist1);*/

        List<myTemporalDocument> myAllDocs = new ArrayList<>(), myAllDocs_Gold = new ArrayList<>();
        int cnt=0;
        for(TemporalDocument d:allDocs){
            String docid = d.getDocID();
            if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                continue;
            myTemporalDocument doc = new myTemporalDocument(d,0);

            myTemporalDocument docGold = new myTemporalDocument(d,1);
            docGold.keepAnchorableEvents(axisMap.get(doc.getDocid()));
            docGold.loadRelationsFromMap(relMap.get(doc.getDocid()),0);

            myAllDocs.add(doc);
            myAllDocs_Gold.add(docGold);

            TempRelAnnotator tra = new TempRelAnnotator(doc,axisLabelerLBJ,tempRelLabelerLBJ,rm,wnsim);
            tra.axisAnnotator();
            tra.tempRelAnnotator(ILP,tempLangMdl.tempLangMdl);
            cnt++;
            if(cnt>=20)
                break;
        }

        myTemporalDocument.NaiveEvaluator(myAllDocs_Gold,myAllDocs,1);
    }
}
