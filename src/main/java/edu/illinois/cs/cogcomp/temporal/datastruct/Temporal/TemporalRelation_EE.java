package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;
import util.TempLangMdl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.startTokInSent;

public class TemporalRelation_EE extends TemporalRelation{
    private int sentDiff, tokDiff;// non-negative
    public boolean e1_covering_e2, e2_covering_e1;
    public String e1_covering_e2_type,e2_covering_e1_type;

    /*Features that may not be initialized*/
    private static TempLangMdl tempLangMdl;
    private HashSet<String> signals_before,signals_between,signals_after;
    private HashSet<String> closestTimexFeats;
    private boolean sameSynset;
    public double c_before, c_after, c_vague, c_equal, c_includes, c_included;

    public TemporalRelation_EE(TemporalRelation_EE other, myTemporalDocument doc){
        super(new EventTemporalNode(other.getSourceNode(),doc), new EventTemporalNode(other.getTargetNode(),doc), new TemporalRelType(other.getRelType()), doc);
        sentDiff = other.sentDiff;
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
        sentDiff = Math.abs(targetNode.getSentId()-sourceNode.getSentId());
        tokDiff = Math.abs(targetNode.getTokenId()-sourceNode.getTokenId());
        checkSRLCovering();
    }

    private HashSet<String> getSignalsFromText(String text, SignalWordSet signalWordSet){
        HashSet<String> ret = getSignalsHelper(text,signalWordSet.temporalSignalSet.connectives_before,"temporalConnectiveSet_before");
        ret.addAll(getSignalsHelper(text,signalWordSet.temporalSignalSet.connectives_after,"temporalConnectiveSet_after"));
        ret.addAll(getSignalsHelper(text,signalWordSet.temporalSignalSet.connectives_during,"temporalConnectiveSet_during"));
        ret.addAll(getSignalsHelper(text,signalWordSet.temporalSignalSet.connectives_contrast,"temporalConnectiveSet_contrast"));
        ret.addAll(getSignalsHelper(text,signalWordSet.temporalSignalSet.connectives_adverb,"temporalConnectiveSet_adverb"));
        ret.addAll(getSignalsHelper(text,signalWordSet.modalVerbSet,"modalVerbSet"));
        ret.addAll(getSignalsHelper(text,signalWordSet.axisSignalWordSet,"axisSignalWordSet"));
        return ret;
    }

    private HashSet<String> getSignalsFromLemma(String Lemma, SignalWordSet signalWordSet){
        HashSet<String> ret = getSignalsHelper(Lemma,signalWordSet.reportingVerbSet,"reportingVerbSet");
        ret.addAll(getSignalsHelper(Lemma,signalWordSet.intentionVerbSet,"intentionVerbSet"));
        return ret;
    }

    private HashSet<String> getSignalsHelper(String text, Set<String> keywords, String keywordsTag){
        HashSet<String> ret = myUtils4TextAnnotation.findKeywordsInText(text, keywords,keywordsTag);
        if(!ret.contains(keywordsTag+":"+"N/A"))
            ret.add(keywordsTag+":EXISTS");
        return ret;
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
    public void extractAllFeats(){
        extractSignalWords();
        readCorpusStats();
        extractIfSameSynset();
        extractClosestTimexFeats();
    }

    public void extractSignalWords(){
        if(signals_before!=null&&signals_between!=null&&signals_after!=null)
            return;
        signals_before = new HashSet<>();
        signals_between = new HashSet<>();
        signals_after = new HashSet<>();
        EventTemporalNode sourceEvent = getSourceNode();
        EventTemporalNode targetEvent = getTargetNode();
        TextAnnotation ta = sourceEvent.getTa();// assume targetEvent.getTa() is the same
        int start = startTokInSent(ta,sourceEvent.getSentId());
        int end = endTokInSent(ta,targetEvent.getSentId());
        int tokId_min = Math.min(sourceEvent.getTokenId(),targetEvent.getTokenId());
        int tokId_max = Math.max(sourceEvent.getTokenId(),targetEvent.getTokenId());

        String text_before = myUtils4TextAnnotation.getSurfaceTextInBetween(ta,start,tokId_min-1);
        String text_between = myUtils4TextAnnotation.getSurfaceTextInBetween(ta,tokId_min+1,tokId_max-1);
        String text_after = myUtils4TextAnnotation.getSurfaceTextInBetween(ta,tokId_max+1,end);
        String lemma_before = myUtils4TextAnnotation.getLemmaTextInBetween(ta,start,tokId_min-1);
        String lemma_between = myUtils4TextAnnotation.getLemmaTextInBetween(ta,tokId_min+1,tokId_max-1);
        String lemma_after = myUtils4TextAnnotation.getLemmaTextInBetween(ta,tokId_max+1,end);

        signals_before = getSignalsFromText(text_before,SignalWordSet.getInstance());
        signals_between = getSignalsFromText(text_between,SignalWordSet.getInstance());
        signals_after = getSignalsFromText(text_after,SignalWordSet.getInstance());
        signals_before.addAll(getSignalsFromLemma(lemma_before,SignalWordSet.getInstance()));
        signals_between.addAll(getSignalsFromLemma(lemma_between,SignalWordSet.getInstance()));
        signals_after.addAll(getSignalsFromLemma(lemma_after,SignalWordSet.getInstance()));
    }

    public void readCorpusStats(){
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

    public void extractIfSameSynset(){
        List<String> e1Synsets = getSourceNode().getSynsets();
        List<String> e2Synsets = getTargetNode().getSynsets();
        Set<String> e1SetSynsets = new HashSet<String>(e1Synsets);
        Set<String> e2SetSynsets = new HashSet<String>(e2Synsets);
        e1SetSynsets.retainAll(e2SetSynsets);
        sameSynset = e1SetSynsets.size() > 0;
    }

    public void extractClosestTimexFeats(){
        if(closestTimexFeats!=null)
            return;
        EventTemporalNode e1 = getSourceNode();
        EventTemporalNode e2 = getTargetNode();
        closestTimexFeats = new HashSet<>();
        closestTimexFeats.addAll(extractClosestTimexFeats_individual(e1,"E1"));
        closestTimexFeats.addAll(extractClosestTimexFeats_individual(e2,"E2"));
        closestTimexFeats.addAll(extractClosestTimexFeats_joint(e1,e2));

    }
    private HashSet<String> extractClosestTimexFeats_individual(EventTemporalNode e,String tag){
        HashSet<String> ret = new HashSet<>();
        TimexTemporalNode t1 = e.getClosestTimex_left();
        TimexTemporalNode t2 = e.getClosestTimex_right();
        if(t1!=null&&!t1.isDCT()){
            ret.add(tag+":"+"ClosestTimex Left:Exist");
            ret.add(tag+":"+"ClosestTimex Left:"+t1.getType());
            if(t1.getSentId()==e.getSentId()){
                ret.add(tag+":"+"ClosestTimex Left:Same Sentence");
                if(e.getTokenId()-t1.getTokenSpan().getSecond()<3)
                    ret.add(tag+":"+"ClosestTimex Left:TokenDiff<3");
                else if(e.getTokenId()-t1.getTokenSpan().getSecond()<5)
                    ret.add(tag+":"+"ClosestTimex Left:TokenDiff<5");
            }
        }
        if(t2!=null&&!t2.isDCT()){
            ret.add(tag+":"+"ClosestTimex Right:Exist");
            ret.add(tag+":"+"ClosestTimex Right:"+t2.getType());
            if(t2.getSentId()==e.getSentId()){
                ret.add(tag+":"+"ClosestTimex Right:Same Sentence");
                if(t2.getTokenSpan().getSecond()-e.getTokenId()<3)
                    ret.add(tag+":"+"ClosestTimex Right:TokenDiff<3");
                else if(t2.getTokenSpan().getSecond()-e.getTokenId()<5)
                    ret.add(tag+":"+"ClosestTimex Right:TokenDiff<5");
            }
        }
        return ret;
    }
    private HashSet<String> extractClosestTimexFeats_joint(EventTemporalNode e1,EventTemporalNode e2){
        HashSet<String> ret = new HashSet<>();
        TimexTemporalNode e1_t1 = e1.getClosestTimex_left();
        TimexTemporalNode e1_t2 = e1.getClosestTimex_right();
        TimexTemporalNode e2_t1 = e2.getClosestTimex_left();
        TimexTemporalNode e2_t2 = e2.getClosestTimex_right();
        if(e1_t1==e2_t1)
            ret.add("E1_E2_JOINT_TIMEX_FEAT:E1 LEFT EQUAL E2 LEFT");
        if(e1_t1==e2_t2)
            ret.add("E1_E2_JOINT_TIMEX_FEAT:E1 LEFT EQUAL E2 RIGHT");
        if(e1_t2==e2_t1)
            ret.add("E1_E2_JOINT_TIMEX_FEAT:E1 RIGHT EQUAL E2 LEFT");
        if(e1_t2==e2_t2)
            ret.add("E1_E2_JOINT_TIMEX_FEAT:E1 RIGHT EQUAL E2 RIGHT");
        return ret;
    }

    /*Getters and setters*/
    @Override
    public TemporalRelation_EE inverse(){
        return new TemporalRelation_EE(getTargetNode(), getSourceNode(),getRelType().inverse(), getDoc());

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

    public int getSentDiff() {
        return sentDiff;
    }

    public int getTokDiff() {
        return tokDiff;
    }

    public HashSet<String> getSignals_before() {
        return signals_before;
    }

    public HashSet<String> getSignals_between() {
        return signals_between;
    }

    public HashSet<String> getSignals_after() {
        return signals_after;
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
