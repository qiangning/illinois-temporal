package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedNode;

import java.io.Serializable;

/**
 * Created by chuchu on 12/20/17.
 */
public class TemporalNode extends AugmentedNode{
    protected int sentId = -1;
    protected TextAnnotation ta;

    public TemporalNode(int nodeId, String nodeType, String text, int sentId) {
        super(nodeId, nodeType, text);
        this.sentId = sentId;
    }

    public TemporalNode(TemporalNode other){
        // Note TextAnnotation is not deeply copied
        this(other.nodeId,other.nodeType,other.text,other.sentId);
        ta = other.ta;
    }

    public int getSentId() {
        return sentId;
    }

    public TextAnnotation getTa() {
        return ta;
    }
}
