package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelation;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelationType;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by chuchu on 12/20/17.
 */
public class TemporalRelation extends BinaryRelation<TemporalNode>{
    private static int LabelMode;
    private myTemporalDocument doc;
    protected int sentDiff;//always positive
    public boolean feat_extraction_done = false;
    /*Features that may not be initialized*/
    protected HashSet<String> signals_before,signals_between,signals_after;
    /*Constructors*/

    public TemporalRelation(TemporalNode sourceNode, TemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType);
        this.doc = doc;
        sentDiff = Math.abs(targetNode.getSentId()-sourceNode.getSentId());
    }

    /*Functions*/

    public void extractAllFeats() {}
    @Override
    public TemporalRelation inverse() {
        TemporalRelation ret = new TemporalRelation(getTargetNode(),getSourceNode(),getRelType().inverse(),doc);
        // since the source & target have been flipped, we need to reverse the scores in reltype
        ret.getRelType().reverseScores();
        return ret;
    }

    protected HashSet<String> getSignalsFromTextSpan(int start, int end, SignalWordSet signalWordSet){
        HashSet<String> ret = getSignalsFromTextSpanHelper(start,end,signalWordSet.temporalSignalSet.connectives_before,"temporalConnectiveSet_before");
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.temporalSignalSet.connectives_after,"temporalConnectiveSet_after"));
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.temporalSignalSet.connectives_during,"temporalConnectiveSet_during"));
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.temporalSignalSet.connectives_contrast,"temporalConnectiveSet_contrast"));
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.temporalSignalSet.connectives_adverb,"temporalConnectiveSet_adverb"));
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.modalVerbSet,"modalVerbSet"));
        ret.addAll(getSignalsFromTextSpanHelper(start,end,signalWordSet.axisSignalWordSet,"axisSignalWordSet"));
        return ret;
    }

    protected HashSet<String> getSignalsFromLemmaSpan(int start, int end, SignalWordSet signalWordSet){
        HashSet<String> ret = getSignalsFromLemmaSpanHelper(start,end,signalWordSet.reportingVerbSet,"reportingVerbSet");
        ret.addAll(getSignalsFromLemmaSpanHelper(start,end,signalWordSet.intentionVerbSet,"intentionVerbSet"));
        return ret;
    }

    protected HashSet<String> getSignalsFromTextSpanHelper(int start, int end, Set<String> keywords, String keywordsTag) {
        return getSignalsFromSpanHelper(start,end,keywords,keywordsTag,doc.keywordLocationsInText);
    }

    protected HashSet<String> getSignalsFromLemmaSpanHelper(int start, int end, Set<String> keywords, String keywordsTag) {
        return getSignalsFromSpanHelper(start,end,keywords,keywordsTag,doc.keywordLocationsInLemma);
    }

    private HashSet<String> getSignalsFromSpanHelper(int start, int end, Set<String> keywords, String keywordsTag, HashMap<String,List<Integer>> keywordsLocationMap) {
        HashSet<String> matches = new HashSet<>();
        boolean flag = false;
        for(String str:keywords){
            if(!keywordsLocationMap.containsKey(str)) continue;
            for(int idx:keywordsLocationMap.get(str)){
                if(idx>end) break;
                if(idx>=start&&idx<=end){
                    flag = true;
                    matches.add(keywordsTag+":"+str.toLowerCase());
                }
            }
        }
        if(!flag)
            matches.add(keywordsTag+":"+"N/A");
        return matches;
    }

    /*Getters and Setters*/
    @Override
    public TemporalRelType getRelType(){
        return (TemporalRelType) super.getRelType();
    }

    public boolean isNull() {
        return getRelType().isNull();
    }

    @Override
    @NotNull
    public TemporalRelation getInverse(){
        return (TemporalRelation)super.getInverse();
    }

    public myTemporalDocument getDoc() {
        return doc;
    }

    public String getLabel(){
        String label = getRelType().getReltype().getName();
        temprelAnnotationReader.Q1_Q2_temprel tuple = new temprelAnnotationReader.Q1_Q2_temprel(new TemporalRelType(label));
        switch (LabelMode){
            case 0:
                return label;
            case 1:
                return tuple.isQ1()?"q1:yes":"q1:no";
            case 2:
                return tuple.isQ2()?"q2:yes":"q2:no";
            default:
                return label;
        }
    }

    public int getSentDiff() {
        return sentDiff;
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

    @Override
    public String toString() {
        return String.format("%s-->%s: %s",getSourceNode().getUniqueId(),getTargetNode().getUniqueId(), getRelType().toString());
    }

    public static void setLabelMode(int labelMode) {
        TemporalRelation.LabelMode = labelMode;
    }
}
