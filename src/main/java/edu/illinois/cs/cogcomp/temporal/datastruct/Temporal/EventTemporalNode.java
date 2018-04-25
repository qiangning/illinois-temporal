package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.*;

public class EventTemporalNode extends TemporalNode{
    private String pos;
    private String lemma;
    private String sense;
    private String cluster;// to-do: frame cluster, or custom cluster
    private int eid;
    private int eiid;
    private int index_in_doc;
    private int tokenId;
    private List<String> synsets;
    private List<String> derivations;// to-do
    private String pp_head;//prepositional phrase head
    private TextAnnotation ta;

    private int window;
    private String[] pos_window;
    private String[] lemma_window;
    private EventTemporalNode prevEvent_SameSynset,nextEvent_SameSynset;

    // used when gold properties are available
    public String tense, aspect, eventclass, polarity;

    public EventTemporalNode(int nodeId, String nodeType, String text, int eid, int eiid, int index_in_doc,int tokenId, TextAnnotation ta) {
        super(nodeId, nodeType, text, ta.getSentenceId(tokenId));
        this.eid = eid;
        this.eiid = eiid;
        this.index_in_doc = index_in_doc;
        this.tokenId = tokenId;
        this.ta = ta;
        pos = retrievePOSAtTokenId(ta,tokenId);
        lemma = retrieveLemmaAtTokenId(ta,tokenId);
        pp_head = retrievePPHeadOfTokenId(ta,tokenId);
    }

    public void extractSynsets(WNSim wnsim){
        synsets = retrieveSynsetUsingLemmaAndPos(wnsim,pos,lemma);
    }

    public void extractPosLemmaWin(int win){
        this.window = win;
        pos_window = retrievePOSWindow(ta,tokenId,win);
        lemma_window = retrieveLemmaWindow(ta,tokenId,win);
    }

    public int getTokenId() {
        return tokenId;
    }

    public int getEiid() {
        return eiid;
    }

    public int getIndex_in_doc() {
        return index_in_doc;
    }

    public String getPp_head() {
        return pp_head;
    }

    public TextAnnotation getTa() {
        return ta;
    }

    public String getPos() {
        return pos;
    }

    public String getLemma() {
        return lemma;
    }

    public List<String> getSynsets() {
        return synsets;
    }

    public void setSynsets(List<String> synsets) {
        this.synsets = synsets;
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

    @Override
    @NotNull
    public String toString() {
        return "EventTemporalNode{" +
                "pos='" + pos + '\'' +
                ", lemma='" + lemma + '\'' +
                ", eiid=" + eiid +
                ", tokenId=" + tokenId +
                ", synsets=" + synsets +
                ", pp_head='" + pp_head + '\'' +
                ", pos_window=" + Arrays.toString(pos_window) +
                ", lemma_window=" + Arrays.toString(lemma_window) +
                '}';
    }
}
