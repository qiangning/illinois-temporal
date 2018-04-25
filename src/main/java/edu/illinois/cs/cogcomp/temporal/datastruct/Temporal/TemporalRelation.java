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
    public TemporalRelType getTemporalRelType(){
        return (TemporalRelType) super.getRelType();
    }

    public boolean isNull() {
        return getTemporalRelType().isNull();
    }

    @Override
    public TemporalRelation inverse() {
        return new TemporalRelation(getTargetNode(),getSourceNode(),(TemporalRelType) getTemporalRelType().inverse());
    }

    public String getLabel(){
        return getTemporalRelType().getReltype().getName();
    }

    @Override
    public String toString() {
        return String.format("%s-->%s: %s",getSourceNode().getUniqueId(),getTargetNode().getUniqueId(), getTemporalRelType().toString());
    }
}
