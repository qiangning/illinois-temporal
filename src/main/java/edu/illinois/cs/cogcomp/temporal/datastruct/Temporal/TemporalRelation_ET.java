package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import org.jetbrains.annotations.NotNull;

public class TemporalRelation_ET extends TemporalRelation {
    public TemporalRelation_ET(EventTemporalNode sourceNode, TimexTemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType, doc);
    }
    public TemporalRelation_ET(TemporalRelation_ET other, myTemporalDocument doc){
        this(other.getSourceNode(),other.getTargetNode(),other.getRelType(),doc);
    }
    @Override
    public EventTemporalNode getSourceNode(){
        return (EventTemporalNode) super.getSourceNode();
    }
    @Override
    public TimexTemporalNode getTargetNode(){
        return (TimexTemporalNode) super.getTargetNode();
    }

    @Override
    @NotNull
    public TemporalRelation_ET getInverse(){
        return (TemporalRelation_ET)super.getInverse();
    }
}
