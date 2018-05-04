package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventTokenCandidate;
import edu.illinois.cs.cogcomp.temporal.configurations.VerbIgnoreSet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import jline.internal.Nullable;

import java.util.*;

import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.*;
import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType.getNullTempRel;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_ON_MAIN_AXIS;

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
                EventTemporalNode tmpNode = new EventTemporalNode(ec.getEiid(), EventNodeType, ec.getText(), ec.getEid(),ec.getEiid(),allEvents.indexOf(ec), tokenId,ta,this);
                addEvent(tmpNode);
            }
            sortAllEvents();

            addTimexFromTemporalJointChunk(temporalDocument.getDocumentCreationTime(),0,true);
            List<TemporalJointChunk> allTimexes = temporalDocument.getBodyTimexMentions();
            for (TemporalJointChunk tjc:allTimexes)
                addTimexFromTemporalJointChunk(tjc,allTimexes.indexOf(tjc)+1,false);
            sortAllTimexes();
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
                TemporalRelation tmpRel = new TemporalRelation(sourceNode, targetNode, reltype,this);
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
            TemporalRelation_EE tmpRel = new TemporalRelation_EE(sourceNode, targetNode, rel, this);
            graph.addRelNoDup(tmpRel);
        }
    }

    public List<EventTokenCandidate> generateAllEventTokenCandidates(int window, HashMap<Integer,String> labelMap){
        // labelMap: (tokenId, converted axis name)
        String[] tokens = ta.getTokens();
        List<EventTokenCandidate> allCandidates = new ArrayList<>();
        EventTokenCandidate prev_event = null;
        for(int i=0;i<tokens.length;i++){
            String label = labelMap.getOrDefault(i,LABEL_NOT_ON_ANY_AXIS);
            EventTokenCandidate etc = new EventTokenCandidate(this,i,label,window,prev_event);
            if(!label.equals(LABEL_NOT_ON_ANY_AXIS))
                prev_event = etc;
            if(etc.getPos().startsWith("V")&&!VerbIgnoreSet.getInstance().verbIgnoreSet.contains(etc.getLemma()))
                allCandidates.add(etc);
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

    private void addTimexFromTemporalJointChunk(TemporalJointChunk tjc, int id, boolean isDCT){
        IntPair tokenSpan;
        int sentId;
        if(isDCT){
            tokenSpan = new IntPair(-1,-1);
            sentId = -1;
        }
        else {
            tokenSpan = new IntPair(ta.getTokenIdFromCharacterOffset(tjc.getCharStart()), ta.getTokenIdFromCharacterOffset(tjc.getCharEnd() - 1) + 1);
            sentId = ta.getSentenceId(tokenSpan.getFirst());

        }
        addTimex(new TimexTemporalNode(tjc.getTID(),TimexNodeType,tjc.getOriginalText(),id,tokenSpan,sentId,isDCT,tjc.getResult().getType(),tjc.getResult().getMod(),tjc.getResult().getValue(),ta));
    }

    public void dropAllEventsAndTimexes(){
        eventList = new ArrayList<>();
        timexList = new ArrayList<>();
        graph.dropAllNodes();
    }

    public void sortAllEvents(){
        eventList.sort(new Comparator<EventTemporalNode>() {
            @Override
            public int compare(EventTemporalNode e1, EventTemporalNode e2) {
                if(e1.getTokenId()<e2.getTokenId())
                    return -1;
                else if(e1.getTokenId()>e2.getTokenId())
                    return 1;
                return 0;
            }
        });
    }

    public void sortAllTimexes(){
        timexList.sort(new Comparator<TimexTemporalNode>() {
            @Override
            public int compare(TimexTemporalNode t1, TimexTemporalNode t2) {
                if(t1.getTokenSpan().getFirst()<t2.getTokenSpan().getFirst())
                    return -1;
                else if(t1.getTokenSpan().getFirst()>t2.getTokenSpan().getFirst())
                    return 1;
                return 0;
            }
        });
    }

    /*Evaluators*/

    public static void NaiveEvaluator(List<myTemporalDocument> doc_gold_list, List<myTemporalDocument> doc_pred_list, int verbose){
        System.out.println("\n"+
                "****************************\n" +
                "*EVALUATING EVENT DETECTION*\n" +
                "****************************\n");
        NaiveEvaluator_EventDetection(doc_gold_list,doc_pred_list,verbose);
        System.out.println("\n"+
                "**************************************************\n" +
                "*EVALUATING EVENT TEMPREL CLASSIFICATION (MODE=0)*\n" +
                "**************************************************\n");
        NaiveEvaluator_TempRelClassification(doc_gold_list,doc_pred_list,0,verbose);
        System.out.println("\n"+
                "**************************************************\n" +
                "*EVALUATING EVENT TEMPREL CLASSIFICATION (MODE=1)*\n" +
                "**************************************************\n");
        NaiveEvaluator_TempRelClassification(doc_gold_list,doc_pred_list,1,verbose);
        System.out.println("\n"+
                "**************************************************\n" +
                "*EVALUATING EVENT TEMPREL CLASSIFICATION (MODE=2)*\n" +
                "**************************************************\n");
        NaiveEvaluator_TempRelClassification(doc_gold_list,doc_pred_list,2,verbose);
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
                String goldLabel = e_gold == null ? LABEL_NOT_ON_ANY_AXIS : LABEL_ON_MAIN_AXIS;
                String predLabel = e_pred == null ? LABEL_NOT_ON_ANY_AXIS : LABEL_ON_MAIN_AXIS;
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

    public static void NaiveEvaluator_TempRelClassification(List<myTemporalDocument> doc_gold_list, List<myTemporalDocument> doc_pred_list, int mode, int verbose){
        // mode: 0--default, 1--ignore gold is null, 2--1+relax vague
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
                if(mode>0&&rel_gold.isNull())
                    continue;
                if(mode==2){
                    if(rel_gold.getReltype() == TemporalRelType.relTypes.VAGUE
                            &&rel_pred.getReltype() != TemporalRelType.relTypes.VAGUE)
                        rel_gold = rel_pred;
                }
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

    public List<TimexTemporalNode> getTimexList() {
        return timexList;
    }

    public String getDocid() {
        return docid;
    }

    public TemporalGraph getGraph() {
        return graph;
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
