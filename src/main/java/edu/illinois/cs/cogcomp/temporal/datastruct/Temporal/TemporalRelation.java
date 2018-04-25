package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelation;

/**
 * Created by chuchu on 12/20/17.
 */
public class TemporalRelation extends BinaryRelation<TemporalNode> {
    /*Constructors*/

    public TemporalRelation(TemporalNode sourceNode, TemporalNode targetNode, TemporalRelType relType) {
        super(sourceNode, targetNode, relType);
    }

    /*Functions*/
    @Override
    public TemporalRelType getRelType(){
        return (TemporalRelType) super.getRelType();
    }

    public boolean isNull() {
        return getRelType().isNull();
    }

    @Override
    public TemporalRelation inverse() {
        return new TemporalRelation(getTargetNode(),getSourceNode(),(TemporalRelType) getRelType().inverse());
    }

    public String getLabel(){
        return getRelType().getReltype().getName();
    }

    @Override
    public String toString() {
        return String.format("%s-->%s: %s",getSourceNode().getUniqueId(),getTargetNode().getUniqueId(), getRelType().toString());
    }
}
