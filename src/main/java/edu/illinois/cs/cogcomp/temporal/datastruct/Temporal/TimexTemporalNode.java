package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;

public class TimexTemporalNode extends TemporalNode{
    private IntPair tokenSpan;
    private boolean isDCT;
    private String type;
    private String mod;
    private String normVal;
    private int index_in_doc;
    public TimexTemporalNode(int nodeId, String nodeType, String text, int index_in_doc, IntPair tokenSpan, int sentId, boolean isDCT, String type, String mod, String normVal, TextAnnotation ta) {
        super(nodeId, nodeType, text, sentId);
        this.index_in_doc = index_in_doc;
        this.tokenSpan = tokenSpan;
        this.isDCT = isDCT;
        this.type = type;
        this.mod = mod;
        this.normVal = normVal;
        this.ta = ta;
    }

    public IntPair getTokenSpan() {
        return tokenSpan;
    }

    public boolean isDCT() {
        return isDCT;
    }

    public String getType() {
        return type==null?"null":type;
    }

    public String getNormVal() {
        return normVal;
    }

    public int getIndex_in_doc() {
        return index_in_doc;
    }

    public String getMod() {
        return mod==null?"null":mod;
    }

    @Override
    public String toString() {
        return "TimexTemporalNode{" +
                "isDCT=" + isDCT +
                ", sentId=" + sentId +
                ", tokenSpan=" + tokenSpan +
                ", text=" + getText()+
                ", type='" + type + '\'' +
                ", mod='" + mod + '\'' +
                ", normVal='" + normVal + '\'' +
                ", index_in_doc=" + index_in_doc +
                '}';
    }
}
