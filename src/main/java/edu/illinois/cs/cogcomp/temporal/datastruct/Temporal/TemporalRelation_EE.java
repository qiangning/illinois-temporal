package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.startTokInSent;

public class TemporalRelation_EE extends TemporalRelation {
    private int sentDiff, tokDiff;// non-negative
    private HashSet<String> signals_before,signals_between,signals_after;
    public double c_before, c_after, c_vague, c_equal, c_includes, c_included;

    public TemporalRelation_EE(EventTemporalNode sourceNode, EventTemporalNode targetNode, TemporalRelType relType) {
        super(sourceNode, targetNode, relType);
        sentDiff = Math.abs(targetNode.getSentId()-sourceNode.getSentId());
        tokDiff = Math.abs(targetNode.getTokenId()-sourceNode.getTokenId());
    }

    @Override
    public TemporalRelation_EE inverse(){
        return new TemporalRelation_EE(getTargetNode(), getSourceNode(),getRelType().inverse());

    }

    @Override
    public EventTemporalNode getSourceNode(){
        return (EventTemporalNode) super.getSourceNode();
    }

    @Override
    public EventTemporalNode getTargetNode(){
        return (EventTemporalNode) super.getTargetNode();
    }

    public void extractSignalWords(){
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
        if(ret.size()>0)
            ret.add(keywordsTag+":Yes");
        return ret;
    }

    public void readCorpusStats(HashMap<String,HashMap<String,HashMap<TLINK.TlinkType,Integer>>> temporalLM){
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

    public boolean sameSynset(){
        List<String> e1Synsets = getSourceNode().getSynsets();
        List<String> e2Synsets = getTargetNode().getSynsets();
        Set<String> e1SetSynsets = new HashSet<String>(e1Synsets);
        Set<String> e2SetSynsets = new HashSet<String>(e2Synsets);
        e1SetSynsets.retainAll(e2SetSynsets);
        return e1SetSynsets.size() > 0;
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
}
