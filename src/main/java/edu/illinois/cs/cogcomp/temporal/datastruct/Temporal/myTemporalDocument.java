package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

public class myTemporalDocument {
    public final static String EventNodeType = "EVENT";
    public final static String TimexNodeType = "TIMEX";
    private List<EventTemporalNode> eventList = new ArrayList<>();
    private List<TimexTemporalNode> timexList = new ArrayList<>();
    private List<TemporalRelation> tempRels_original = new ArrayList<>();
    private TextAnnotation ta;
    private TemporalGraph graph;
    private String docid;

    public myTemporalDocument(String bodytext, String docid){
        //to-do

        // create TA

        // create timexList

        // create eventList

        // create graph
    }

    public myTemporalDocument(TemporalDocument temporalDocument){// to-do
        docid = temporalDocument.getDocID();
        ta = temporalDocument.getTextAnnotation();
        graph = new TemporalGraph();
        for(EventChunk ec:temporalDocument.getBodyEventMentions()){
            int tokenId = ta.getTokenIdFromCharacterOffset(ec.getCharStart());
            int sentId = ta.getSentenceId(tokenId);
            EventTemporalNode tmpNode = new EventTemporalNode(ec.getEiid(),EventNodeType,ec.getText(),tokenId,sentId);
            eventList.add(tmpNode);
            graph.addNodeNoDup(tmpNode);
        }
        List<TemporalJointChunk> allTimexes = temporalDocument.getBodyTimexMentions();
        allTimexes.add(temporalDocument.getDocumentCreationTime());
        for(TemporalJointChunk tjc:allTimexes){
            TimexTemporalNode tmpNode = new TimexTemporalNode(tjc.getTID(),TimexNodeType,tjc.getOriginalText(),-1);
            timexList.add(tmpNode);
            graph.addNodeNoDup(tmpNode);
        }
        for(TLINK tlink:temporalDocument.getBodyTlinks()){
            TemporalNode sourceNode = (TemporalNode) graph.getNode((tlink.getSourceType().equals(TempEval3Reader.Type_Event)?EventNodeType:TimexNodeType)+":"+tlink.getSourceId());
            TemporalNode targetNode = (TemporalNode) graph.getNode((tlink.getTargetType().equals(TempEval3Reader.Type_Event)?EventNodeType:TimexNodeType)+":"+tlink.getTargetId());
            TemporalRelType reltype = TemporalRelType.getNull();
            switch(tlink.getReducedRelType().toStringfull()){
                case "before":
                case "includes":
                    reltype = new TemporalRelType(TemporalRelType.relTypes.BEFORE);break;
                case "after":
                case "included":
                    reltype = new TemporalRelType(TemporalRelType.relTypes.AFTER);break;
                case "equal":
                    reltype = new TemporalRelType(TemporalRelType.relTypes.EQUAL);break;
                default:
                    reltype = new TemporalRelType(TemporalRelType.relTypes.VAGUE);
            }
            if(sourceNode==null||targetNode==null)
                System.out.println();
            TemporalRelation tmpRel = new TemporalRelation(sourceNode,targetNode,reltype);
            graph.addRelNoDup(tmpRel);
        }
    }

    public TextAnnotation getTextAnnotation() {
        return ta;
    }

    public List<EventTemporalNode> getEventList() {
        return eventList;
    }

    public String getDocid() {
        return docid;
    }

    public static void main(String[] args) throws Exception{
        String dir = new temporalConfigurator().getConfig("config/directory.properties").getString("TBDense_Ser");
        List<TemporalDocument> tbdense = TempEval3Reader.deserialize(dir);
        myTemporalDocument doc = new myTemporalDocument(tbdense.get(0));
        System.out.println();
    }
}
