package edu.illinois.cs.cogcomp.temporal.eventAxisDetector;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;

import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.LABEL_NOT_ON_ANY_AXIS;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.*;

public class EventTokenCandidate {
    /*public static HashMap<String, float[]> label_map = new HashMap<String, float[]>(){{
        put("main",new float[]{1f,0f,0f});
        put("others",new float[]{0f,1f,0f});
        put("null",new float[]{0f,0f,1f});
    }};*/
    private String label;//event axis label or not an event
    private int tokenId,sentId;
    private String pos;
    private String lemma;
    private String pp_head;

    private int window;
    private String[] pos_window;
    private String[] lemma_window;

    private HashSet<String> signals_before;
    private HashSet<String> signals_after;

    /*private List<String> synsets;
    private List<String> derivations;*/

    private myTemporalDocument doc;

    public EventTokenCandidate(myTemporalDocument doc,int tokenId, String label, int window) {
        this.doc = doc;
        this.tokenId = tokenId;
        this.label = label;
        this.window = window;
        if(window<0){
            System.out.println("[WARNING] Window cannot be negative; reset to 0.");
            window = 0;
        }
        TextAnnotation ta = doc.getTextAnnotation();
        if(!isTokenIdValid(ta,tokenId)) return;
        sentId = ta.getSentenceId(tokenId);
        // POS
        this.pos = retrievePOSAtTokenId(ta,tokenId);
        // Lemma
        this.lemma = retrieveLemmaAtTokenId(ta,tokenId);
        // PP Head
        this.pp_head = retrievePPHeadOfTokenId(ta,tokenId);
        // POS in a window
        pos_window = retrievePOSWindow(ta,tokenId,window);
        // Lemma in a window
        lemma_window = retrieveLemmaWindow(ta,tokenId,window);
        // Signal words before and after tokenId within the same sentence
        signals_before = new HashSet<>();
        signals_after = new HashSet<>();
        int start = startTokInSent(doc.getTextAnnotation(),sentId);
        int end = endTokInSent(ta,sentId);
        String text_before = myUtils4TextAnnotation.getSurfaceTextInBetween(ta,start,tokenId-1);
        String text_after = myUtils4TextAnnotation.getSurfaceTextInBetween(ta,tokenId+1,end);
        String lemma_before = myUtils4TextAnnotation.getLemmaTextInBetween(ta,start,tokenId-1);
        String lemma_after = myUtils4TextAnnotation.getLemmaTextInBetween(ta,tokenId+1,end);
        signals_before = myUtils4TextAnnotation.findKeywordsInText(text_before, SignalWordSet.getInstance().temporalConnectiveSet,"TemporalConnective");
        signals_after = myUtils4TextAnnotation.findKeywordsInText(text_after, SignalWordSet.getInstance().temporalConnectiveSet,"TemporalConnective");
        signals_before.addAll(myUtils4TextAnnotation.findKeywordsInText(text_before, SignalWordSet.getInstance().modalVerbSet,"modalVerbSet"));
        signals_after.addAll(myUtils4TextAnnotation.findKeywordsInText(text_after, SignalWordSet.getInstance().modalVerbSet,"modalVerbSet"));
        signals_before.addAll(myUtils4TextAnnotation.findKeywordsInText(text_before, SignalWordSet.getInstance().axisSignalWordSet,"axisSignalWordSet"));
        signals_after.addAll(myUtils4TextAnnotation.findKeywordsInText(text_after, SignalWordSet.getInstance().axisSignalWordSet,"axisSignalWordSet"));
        signals_before.addAll(myUtils4TextAnnotation.findKeywordsInText(lemma_before, SignalWordSet.getInstance().reportingVerbSet,"reportingVerbSet"));
        signals_after.addAll(myUtils4TextAnnotation.findKeywordsInText(lemma_after, SignalWordSet.getInstance().reportingVerbSet,"reportingVerbSet"));
    }

    public static List<EventTokenCandidate> generateAllEventTokenCandidates(myTemporalDocument doc, int window, HashMap<Integer,String> labelMap){
        String[] tokens = doc.getTextAnnotation().getTokens();
        List<EventTokenCandidate> allCandidates = new ArrayList<>();
        for(int i=0;i<tokens.length;i++){
            String label = labelMap.getOrDefault(i,LABEL_NOT_ON_ANY_AXIS);
            allCandidates.add(new EventTokenCandidate(doc,i,label,window));
        }
        return allCandidates;
    }

    public String getLabel() {
        return label;
    }

    /*public float[] getLabelArray(){return label_map.get(label);}*/

    public int getTokenId() {
        return tokenId;
    }

    public int getSentId() {
        return sentId;
    }

    public String getPos() {
        return pos;
    }

    public String getLemma() {
        return lemma;
    }

    public String getPp_head() {
        return pp_head;
    }

    public int getWindow() {
        return window;
    }

    public String[] getPos_window() {
        return pos_window;
    }

    public String[] getLemma_window() {
        return lemma_window;
    }

    public HashSet<String> getSignals_before() {
        return signals_before;
    }

    public HashSet<String> getSignals_after() {
        return signals_after;
    }

    public myTemporalDocument getDoc() {
        return doc;
    }
}
