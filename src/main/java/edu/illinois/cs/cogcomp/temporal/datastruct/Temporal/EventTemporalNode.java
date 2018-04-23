package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    private String[] prev_pos;
    private String[] next_pos;
    private EventTemporalNode prevEvent_SameSynset,nextEvent_SameSynset;

    // used when gold properties are available
    public String tense, aspect, eventclass, polarity;

    public EventTemporalNode(int nodeId, String nodeType, String text, int eid, int eiid, int index_in_doc,int tokenId, int sentId) {
        super(nodeId, nodeType, text, sentId);
        this.eid = eid;
        this.eiid = eiid;
        this.index_in_doc = index_in_doc;
        this.tokenId = tokenId;
    }

    @Override
    @NotNull
    public String toString() {
        return "Event{" +
                "text='" + getText() + '\'' +
                ", lemma='" + lemma + '\'' +
                ", sense='" + sense + '\'' +
                ", cluster='" + cluster + '\'' +
                ", eid=" + eid +
                ", eiid=" + eiid +
                ", index_in_doc=" + index_in_doc +
                ", tokenId=" + tokenId +
                ", sentId=" + getSentId() +
                '}';
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
}
