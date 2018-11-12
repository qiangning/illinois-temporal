package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;
import org.jetbrains.annotations.NotNull;
import util.TempLangMdl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.EventTemporalNode.extractClosestTimexFeats_joint;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.startTokInSent;

public class TemporalRelation_EE extends TemporalRelation{
    private int tokDiff;// non-negative
    public boolean e1_covering_e2, e2_covering_e1;
    public String e1_covering_e2_type,e2_covering_e1_type;
    public static boolean useTemProb=true;
    public double c_before=1, c_after=1, c_vague=1, c_equal=1, c_includes=1, c_included=1;


    /*Features that may not be initialized*/
    private static TempLangMdl tempLangMdl;
    private HashSet<String> closestTimexFeats;
    private boolean sameSynset;

    public TemporalRelation_EE(TemporalRelation_EE other, myTemporalDocument doc){
        super(new EventTemporalNode(other.getSourceNode(),doc), new EventTemporalNode(other.getTargetNode(),doc), new TemporalRelType(other.getRelType()), doc);
        tokDiff = other.tokDiff;
        e1_covering_e2 = other.e1_covering_e2;
        e2_covering_e1 = other.e2_covering_e1;
        e1_covering_e2_type  = other.e1_covering_e2_type;
        e2_covering_e1_type  = other.e2_covering_e1_type;
        signals_before = other.signals_before;
        signals_between = other.signals_between;
        signals_after = other.signals_after;
        closestTimexFeats = other.closestTimexFeats;
        sameSynset = other.sameSynset;
        c_before = other.c_before;
        c_after = other.c_after;
        c_vague = other.c_vague;
        c_equal = other.c_equal;
        c_includes = other.c_includes;
        c_included = other.c_included;
    }
    public TemporalRelation_EE(EventTemporalNode sourceNode, EventTemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType, doc);
        tokDiff = Math.abs(targetNode.getTokenId()-sourceNode.getTokenId());
        checkSRLCovering();
    }

    private void checkSRLCovering(){
        EventTemporalNode e1 = getSourceNode();
        EventTemporalNode e2 = getTargetNode();
        e1_covering_e2 = false;
        e2_covering_e1 = false;
        e1_covering_e2_type  = "N/A";
        e2_covering_e1_type  = "N/A";
        List<Pair<String,Constituent>> allSRL = e2.getVerb_srl_covering();
        for(Pair p:allSRL){
            if(p.getSecond()==e1.getVerb_srl()) {
                e1_covering_e2 = true;
                e1_covering_e2_type = (String) p.getFirst();
            }
        }

        allSRL = e1.getVerb_srl_covering();
        for(Pair p:allSRL){
            if(p.getSecond()==e2.getVerb_srl()) {
                e2_covering_e1 = true;
                e2_covering_e1_type = (String) p.getFirst();
            }
        }
    }

    /*Feature extraction*/
    @Override
    public void extractAllFeats(){
        feat_extraction_done = true;
        extractSignalWords();
        if(useTemProb) readCorpusStats();
        extractIfSameSynset();
        extractClosestTimexFeats();
    }

    private void extractSignalWords(){
        if(signals_before!=null&&signals_between!=null&&signals_after!=null)
            return;
        signals_before = new HashSet<>();
        signals_between = new HashSet<>();
        signals_after = new HashSet<>();
        EventTemporalNode sourceEvent = getSourceNode();
        EventTemporalNode targetEvent = getTargetNode();
        TextAnnotation ta = sourceEvent.getTa();// assume targetEvent.getTa() is the same
        int start = startTokInSent(ta,Math.min(sourceEvent.getSentId(),targetEvent.getSentId()));
        int end = endTokInSent(ta,Math.max(sourceEvent.getSentId(),targetEvent.getSentId()));
        int tokId_min = Math.min(sourceEvent.getTokenId(),targetEvent.getTokenId());
        int tokId_max = Math.max(sourceEvent.getTokenId(),targetEvent.getTokenId());

        signals_before = getSignalsFromTextSpan(start,tokId_min-1,SignalWordSet.getInstance());
        signals_between = getSignalsFromTextSpan(tokId_min+1,tokId_max-1,SignalWordSet.getInstance());
        signals_after = getSignalsFromTextSpan(tokId_max+1,end,SignalWordSet.getInstance());
        signals_before.addAll(getSignalsFromLemmaSpan(start,tokId_min-1,SignalWordSet.getInstance()));
        signals_between.addAll(getSignalsFromLemmaSpan(tokId_min+1,tokId_max-1,SignalWordSet.getInstance()));
        signals_after.addAll(getSignalsFromLemmaSpan(tokId_max+1,end,SignalWordSet.getInstance()));
    }

    private void readCorpusStats(){
        HashMap<String,HashMap<String,HashMap<TLINK.TlinkType,Integer>>> temporalLM = getTempLangMdl().tempLangMdl;
        if(temporalLM.containsKey(getSourceNode().getCluster())&&temporalLM.get(getSourceNode().getCluster()).containsKey(getTargetNode().getCluster())){
            String cluster1 = getSourceNode().getCluster();
            String cluster2 = getTargetNode().getCluster();
            HashMap<TLINK.TlinkType,Integer> tmp = temporalLM.get(cluster1).get(cluster2);
            c_before = tmp.getOrDefault(TLINK.TlinkType.BEFORE,0)+1;
            c_after = tmp.getOrDefault(TLINK.TlinkType.AFTER,0)+1;
            c_includes = tmp.getOrDefault(TLINK.TlinkType.INCLUDES,0)+1;
            c_included = tmp.getOrDefault(TLINK.TlinkType.IS_INCLUDED,0)+1;
            c_equal = tmp.getOrDefault(TLINK.TlinkType.EQUAL,0)+1;
            c_vague = tmp.getOrDefault(TLINK.TlinkType.UNDEF,0)+1;
        }
        else{
            c_before = 1;
            c_after = 1;
            c_includes = 1;
            c_included = 1;
            c_vague = 1;
            c_equal = 1;
        }
    }

    private void extractIfSameSynset(){
        List<String> e1Synsets = getSourceNode().getSynsets();
        List<String> e2Synsets = getTargetNode().getSynsets();
        Set<String> e1SetSynsets = new HashSet<String>(e1Synsets);
        Set<String> e2SetSynsets = new HashSet<String>(e2Synsets);
        e1SetSynsets.retainAll(e2SetSynsets);
        sameSynset = e1SetSynsets.size() > 0;
    }

    private void extractClosestTimexFeats(){
        if(closestTimexFeats!=null)
            return;
        EventTemporalNode e1 = getSourceNode();
        EventTemporalNode e2 = getTargetNode();
        closestTimexFeats = new HashSet<>();
        closestTimexFeats.addAll(e1.extractClosestTimexFeats_individual("E1"));
        closestTimexFeats.addAll(e2.extractClosestTimexFeats_individual("E2"));
        closestTimexFeats.addAll(extractClosestTimexFeats_joint(e1,e2));
    }

    /*Getters and setters*/
    @Override
    public TemporalRelation_EE inverse(){
        TemporalRelation_EE ret = new TemporalRelation_EE(getTargetNode(),getSourceNode(),getRelType().inverse(),getDoc());
        // since the source & target have been flipped, we need to reverse the scores in reltype
        ret.getRelType().reverseScores();
        return ret;
    }

    @Override
    @NotNull
    public TemporalRelation_EE getInverse(){
        return (TemporalRelation_EE)super.getInverse();
    }

    @Override
    public EventTemporalNode getSourceNode(){
        return (EventTemporalNode) super.getSourceNode();
    }

    @Override
    public EventTemporalNode getTargetNode(){
        return (EventTemporalNode) super.getTargetNode();
    }

    public boolean isSameSynset() {
        return sameSynset;
    }

    public int getTokDiff() {
        return tokDiff;
    }

    public HashSet<String> getClosestTimexFeats() {
        return closestTimexFeats;
    }

    public static TempLangMdl getTempLangMdl() {
        if(tempLangMdl==null){
            try {
                ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
                String lm_path = rm.getString("TemProb_Dir");
                setTempLangMdl(TempLangMdl.getInstance(lm_path));
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("Temporal Language Model (TemProb) loading error. Exiting now.");
                System.exit(-1);
            }
        }
        return tempLangMdl;
    }

    public static void setTempLangMdl(TempLangMdl tempLangMdl) {
        TemporalRelation_EE.tempLangMdl = tempLangMdl;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)-->%s (%s): %s. sameSynset:%s. e1_covering_e2:%s, e2_covering_e1:%s",getSourceNode().getUniqueId(),getSourceNode().getLemma(),getTargetNode().getUniqueId(),getTargetNode().getLemma(), getRelType().toString(),sameSynset,e1_covering_e2,e2_covering_e1);
    }
}
