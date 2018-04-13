package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedNode;

/**
 * Created by chuchu on 12/20/17.
 */
public class TemporalNode extends AugmentedNode {
    private int sentId = -1;
    public TemporalNode(int nodeId, String nodeType, String text, int sentId) {
        super(nodeId, nodeType, text);
        this.sentId = sentId;
    }

    public int getSentId() {
        return sentId;
    }
}
