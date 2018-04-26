package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventTokenCandidate;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import jline.internal.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.*;
import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType.getNullTempRel;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class myTemporalDocument {
    public final static String EventNodeType = "EVENT";
    public final static String TimexNodeType = "TIMEX";
    private List<EventTemporalNode> eventList = new ArrayList<>();
    private List<TimexTemporalNode> timexList = new ArrayList<>();
    private TextAnnotation ta;
    private TemporalGraph graph;
    private String docid;
    private HashMap<Integer,EventTemporalNode> map_tokenId2event = new HashMap<>();

    public myTemporalDocument(String bodytext, String docid){
        //to-do

        // create TA

        // create timexList

        // create eventList

        // create graph
    }
    public myTemporalDocument(TemporalDocument temporalDocument){
        this(temporalDocument,2);
    }
    public myTemporalDocument(TemporalDocument temporalDocument, int mode){
        // mode: 0-->don't load events, timexes, and relations
        // mode: 1-->load events and timexes in the original temporalDocument
        // mode: 2-->load relations in the original temporalDocument (this should be rare)
        docid = temporalDocument.getDocID();
        ta = temporalDocument.getTextAnnotation();
        graph = new TemporalGraph();
        if(mode>=1) {
            List<EventChunk> allEvents = temporalDocument.getBodyEventMentions();
            for (EventChunk ec : allEvents) {
                int tokenId = ta.getTokenIdFromCharacterOffset(ec.getCharStart());
                EventTemporalNode tmpNode = new EventTemporalNode(ec.getEiid(), EventNodeType, ec.getText(), ec.getEid(),ec.getEiid(),allEvents.indexOf(ec), tokenId,ta);
                addEvent(tmpNode);
            }
            List<TemporalJointChunk> allTimexes = temporalDocument.getBodyTimexMentions();
            allTimexes.add(temporalDocument.getDocumentCreationTime());
            for (TemporalJointChunk tjc : allTimexes) {
                TimexTemporalNode tmpNode = new TimexTemporalNode(tjc.getTID(), TimexNodeType, tjc.getOriginalText(), -1);
                addTimex(tmpNode);
            }
        }
        if(mode>=2) {
            for (TLINK tlink : temporalDocument.getBodyTlinks()) {
                TemporalNode sourceNode = graph.getNode((tlink.getSourceType().equals(TempEval3Reader.Type_Event) ? EventNodeType : TimexNodeType) + ":" + tlink.getSourceId());
                TemporalNode targetNode = graph.getNode((tlink.getTargetType().equals(TempEval3Reader.Type_Event) ? EventNodeType : TimexNodeType) + ":" + tlink.getTargetId());
                TemporalRelType reltype;
                switch (tlink.getReducedRelType().toStringfull()) {
                    case "before":
                    case "includes":
                        reltype = new TemporalRelType(TemporalRelType.relTypes.BEFORE);
                        break;
                    case "after":
                    case "included":
                        reltype = new TemporalRelType(TemporalRelType.relTypes.AFTER);
                        break;
                    case "equal":
                        reltype = new TemporalRelType(TemporalRelType.relTypes.EQUAL);
                        break;
                    default:
                        reltype = new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                }
                if (sourceNode == null || targetNode == null)
                    System.out.println();
                TemporalRelation tmpRel = new TemporalRelation(sourceNode, targetNode, reltype);
                graph.addRelNoDup(tmpRel);
            }
        }
    }

    public void keepAnchorableEvents(HashMap<Integer,String> axisMap){
        // axisMap: (index in doc, CrowdFlower axis name)
        List<EventTemporalNode> newEventList = new ArrayList<>();
        for(EventTemporalNode e:eventList){
            if(!axisMap.containsKey(eventList.indexOf(e))
                    ||!axisMap.get(eventList.indexOf(e)).equals("yes_its_anchorable")) {
                graph.dropNode(EventNodeType+":"+e.getEiid());
            }
            else{
                newEventList.add(e);
            }
        }
        eventList = newEventList;
    }

    public void loadRelationsFromMap(List<temprelAnnotationReader.CrowdFlowerEntry> relMap, int verbose){
        // currently, relMap is EE only
        for(temprelAnnotationReader.CrowdFlowerEntry entry:relMap){
            int eiid1 = entry.getEventid1();
            int eiid2 = entry.getEventid2();
            TemporalRelType rel = entry.getRel().getRelType();
            EventTemporalNode sourceNode = (EventTemporalNode) graph.getNode(EventNodeType+":"+eiid1);
            EventTemporalNode targetNode = (EventTemporalNode) graph.getNode(EventNodeType+":"+eiid2);
            if(sourceNode==null||targetNode==null){
                if(verbose>0)
                    System.out.printf("[WARNING] null node in graph %s: %s\n", docid,entry.toString());
                continue;
            }
            TemporalRelation_EE tmpRel = new TemporalRelation_EE(sourceNode, targetNode, rel);
            graph.addRelNoDup(tmpRel);
        }
    }

    public List<EventTokenCandidate> generateAllEventTokenCandidates(int window, HashMap<Integer,String> labelMap){
        // labelMap: (tokenId, converted axis name)
        String[] tokens = ta.getTokens();
        List<EventTokenCandidate> allCandidates = new ArrayList<>();
        for(int i=0;i<tokens.length;i++){
            String label = labelMap.getOrDefault(i,LABEL_NOT_ON_ANY_AXIS);
            allCandidates.add(new EventTokenCandidate(this,i,label,window));
        }
        return allCandidates;
    }
    public void addEvent(EventTemporalNode e){
        eventList.add(e);
        graph.addNodeNoDup(e);
    }
    public void addTimex(TimexTemporalNode t){
        timexList.add(t);
        graph.addNodeNoDup(t);
    }

    public void dropAllEventsAndTimexes(){
        eventList = new ArrayList<>();
        timexList = new ArrayList<>();
        graph.dropAllNodes();
    }

    @Nullable
    public EventTemporalNode getEventFromTokenId(int tokenId){
        if(map_tokenId2event==null||map_tokenId2event.size()==0) {
            map_tokenId2event = new HashMap<>();
            for(EventTemporalNode e:eventList){
                map_tokenId2event.put(e.getTokenId(),e);
            }
        }
        return map_tokenId2event.get(tokenId);
    }

    public TemporalRelType getEERelFromTokenIds(int tokenId1, int tokenId2){
        EventTemporalNode e1 = getEventFromTokenId(tokenId1);
        EventTemporalNode e2 = getEventFromTokenId(tokenId2);
        if(e1==null||e2==null)
            return getNullTempRel();
        TemporalRelation ee_rel = graph.getRelBetweenNodes(e1.getUniqueId(),e2.getUniqueId());
        if(ee_rel==null)
            return getNullTempRel();
        return ee_rel.getRelType();
    }

    public static void NaiveEvaluator(List<myTemporalDocument> doc_gold_list, List<myTemporalDocument> doc_pred_list, int verbose){
        System.out.println(
                "****************************\n" +
                "*EVALUATING EVENT DETECTION*\n" +
                "****************************\n");
        NaiveEvaluator_EventDetection(doc_gold_list,doc_pred_list,verbose);
        System.out.println(
                "*****************************************\n" +
                "*EVALUATING EVENT TEMPREL CLASSIFICATION*\n" +
                "*****************************************\n");
        NaiveEvaluator_TempRelClassification(doc_gold_list,doc_pred_list,verbose);
    }

    public static void NaiveEvaluator_EventDetection(List<myTemporalDocument> doc_gold_list, List<myTemporalDocument> doc_pred_list, int verbose){
        PrecisionRecallManager eventDetectorEvaluator = new PrecisionRecallManager();
        PrecisionRecallManager eventDetectorEvaluatorDetail;
        if(doc_gold_list.size()!=doc_pred_list.size()){
            System.out.println("[WARNING] doc_gold_list and doc_pred_list don't match.");
            return;
        }
        for(int k=0;k<doc_gold_list.size();k++) {
            eventDetectorEvaluatorDetail = new PrecisionRecallManager();
            myTemporalDocument doc_gold = doc_gold_list.get(k);
            myTemporalDocument doc_pred = doc_pred_list.get(k);
            // check
            if(!doc_gold.getDocid().equals(doc_pred.getDocid())){
                System.out.println("[WARNING] doc_gold_list and doc_pred_list don't match.");
                return;
            }

            int tokenSize = doc_gold.getTextAnnotation().getTokens().length;
            for (int i = 0; i < tokenSize; i++) {
                EventTemporalNode e_gold = doc_gold.getEventFromTokenId(i);
                EventTemporalNode e_pred = doc_pred.getEventFromTokenId(i);
                String goldLabel = e_gold == null ? LABEL_NOT_ON_ANY_AXIS : "main";
                String predLabel = e_pred == null ? LABEL_NOT_ON_ANY_AXIS : "main";
                eventDetectorEvaluator.addPredGoldLabels(predLabel, goldLabel);
                eventDetectorEvaluatorDetail.addPredGoldLabels(predLabel, goldLabel);
            }
            if(verbose>1) {
                System.out.printf("--------#%d Doc: %s--------\n",k,doc_gold.getDocid());
                eventDetectorEvaluatorDetail.printPrecisionRecall(EventAxisPerceptronTrainer.AXIS_LABEL_TO_IGNORE);
                if (verbose > 2) {
                    System.out.println("----------CONFUSION MATRIX----------");
                    eventDetectorEvaluatorDetail.printConfusionMatrix();
                }
            }
        }
        System.out.printf("########Evaluation of %d documents########\n",doc_gold_list.size());
        eventDetectorEvaluator.printPrecisionRecall(EventAxisPerceptronTrainer.AXIS_LABEL_TO_IGNORE);
        if (verbose > 0) {
            System.out.println("----------CONFUSION MATRIX----------");
            eventDetectorEvaluator.printConfusionMatrix();
        }
    }

    public static void NaiveEvaluator_TempRelClassification(List<myTemporalDocument> doc_gold_list, List<myTemporalDocument> doc_pred_list, int verbose){
        PrecisionRecallManager tempRelClsEvaluator = new PrecisionRecallManager();
        PrecisionRecallManager tempRelClsEvaluatorDetail;
        if(doc_gold_list.size()!=doc_pred_list.size()){
            System.out.println("[WARNING] doc_gold_list and doc_pred_list don't match.");
            return;
        }
        for(int k=0;k<doc_gold_list.size();k++) {
            tempRelClsEvaluatorDetail = new PrecisionRecallManager();
            myTemporalDocument doc_gold = doc_gold_list.get(k);
            myTemporalDocument doc_pred = doc_pred_list.get(k);
            // check
            if(!doc_gold.getDocid().equals(doc_pred.getDocid())){
                System.out.println("[WARNING] doc_gold_list and doc_pred_list don't match.");
                return;
            }

            // Get all EE pairs set
            HashSet<String> allEEPairs_str = new HashSet<>();// format: event_tokenId1 + ":" + event_tokenId2
            List<TemporalRelation_EE> allEE = doc_gold.getGraph().getAllEERelations(-1);
            allEE.addAll(doc_pred.getGraph().getAllEERelations(-1));
            for(TemporalRelation_EE ee:allEE){
                int tokId1 = ee.getSourceNode().getTokenId();
                int tokId2 = ee.getTargetNode().getTokenId();
                if(tokId1>tokId2) continue;
                allEEPairs_str.add(tokId1+":"+tokId2);
            }

            // Evaluate all EE pairs
            for(String ee_str:allEEPairs_str){
                String[] tmp = ee_str.split(":");
                int tokId1 = Integer.valueOf(tmp[0]);
                int tokId2 = Integer.valueOf(tmp[1]);
                TemporalRelType rel_gold = doc_gold.getEERelFromTokenIds(tokId1,tokId2);
                TemporalRelType rel_pred = doc_pred.getEERelFromTokenIds(tokId1,tokId2);
                if(rel_gold.isNull())
                    continue;
                tempRelClsEvaluator.addPredGoldLabels(rel_pred.getReltype().getName(), rel_gold.getReltype().getName());
                tempRelClsEvaluatorDetail.addPredGoldLabels(rel_pred.getReltype().getName(), rel_gold.getReltype().getName());
            }
            if(verbose>1) {
                System.out.printf("--------#%d Doc: %s--------\n",k,doc_gold.getDocid());
                tempRelClsEvaluatorDetail.printPrecisionRecall(EventTemprelPerceptronTrainer.TEMP_LABEL_TO_IGNORE);
                if (verbose > 2) {
                    System.out.println("----------CONFUSION MATRIX----------");
                    tempRelClsEvaluatorDetail.printConfusionMatrix();
                }
            }
        }
        System.out.printf("########Evaluation of %d documents########\n",doc_gold_list.size());
        tempRelClsEvaluator.printPrecisionRecall(EventTemprelPerceptronTrainer.TEMP_LABEL_TO_IGNORE);
        if (verbose > 0) {
            System.out.println("----------CONFUSION MATRIX----------");
            tempRelClsEvaluator.printConfusionMatrix();
        }
    }
    /*Getters and Setters*/
    public TextAnnotation getTextAnnotation() {
        return ta;
    }

    public List<EventTemporalNode> getEventList() {
        return eventList;
    }

    public String getDocid() {
        return docid;
    }

    public TemporalGraph getGraph() {
        return graph;
    }

    public static void main(String[] args) throws Exception{
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        String dir = rm.getString("TBDense_Ser");
        List<TemporalDocument> docs = TempEval3Reader.deserialize(dir);
        myTemporalDocument doc = new myTemporalDocument(docs.get(0),1);

        HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
        doc.keepAnchorableEvents(axisMap.get(doc.getDocid()));
        HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));
        doc.loadRelationsFromMap(relMap.get(doc.getDocid()),1);
        System.out.println();
    }
}
