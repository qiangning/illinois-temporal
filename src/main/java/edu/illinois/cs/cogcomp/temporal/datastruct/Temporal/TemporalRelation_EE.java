package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.startTokInSent;

public class TemporalRelation_EE extends TemporalRelation {
    private int sentDiff, tokDiff;// non-negative
    private HashSet<String> signals_before,signals_between,signals_after;

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
        signals_before = myUtils4TextAnnotation.findKeywordsInText(text_before, SignalWordSet.getInstance().temporalConnectiveSet,"TemporalConnective");
        signals_between = myUtils4TextAnnotation.findKeywordsInText(text_between,SignalWordSet.getInstance().temporalConnectiveSet,"TemporalConnective");
        signals_after = myUtils4TextAnnotation.findKeywordsInText(text_after, SignalWordSet.getInstance().temporalConnectiveSet,"TemporalConnective");

        signals_before.addAll(myUtils4TextAnnotation.findKeywordsInText(text_before, SignalWordSet.getInstance().modalVerbSet,"modalVerbSet"));
        signals_between.addAll(myUtils4TextAnnotation.findKeywordsInText(text_between, SignalWordSet.getInstance().modalVerbSet,"modalVerbSet"));
        signals_after.addAll(myUtils4TextAnnotation.findKeywordsInText(text_after, SignalWordSet.getInstance().modalVerbSet,"modalVerbSet"));

        signals_before.addAll(myUtils4TextAnnotation.findKeywordsInText(lemma_before, SignalWordSet.getInstance().reportingVerbSet,"reportingVerbSet"));
        signals_between.addAll(myUtils4TextAnnotation.findKeywordsInText(lemma_between, SignalWordSet.getInstance().reportingVerbSet,"reportingVerbSet"));
        signals_after.addAll(myUtils4TextAnnotation.findKeywordsInText(lemma_after, SignalWordSet.getInstance().reportingVerbSet,"reportingVerbSet"));
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
