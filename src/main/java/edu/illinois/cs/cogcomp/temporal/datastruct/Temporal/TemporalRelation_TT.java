package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import org.jetbrains.annotations.NotNull;

public class TemporalRelation_TT extends TemporalRelation{
    public TemporalRelation_TT(TimexTemporalNode sourceNode, TimexTemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType, doc);
    }
    public TemporalRelation_TT(TemporalRelation_TT other, myTemporalDocument doc){
        this(other.getSourceNode(),other.getTargetNode(),other.getRelType(),doc);
    }
    @Override
    public TimexTemporalNode getSourceNode(){
        return (TimexTemporalNode) super.getSourceNode();
    }
    @Override
    public TimexTemporalNode getTargetNode(){
        return (TimexTemporalNode) super.getTargetNode();
    }
    @Override
    @NotNull
    public TemporalRelation_TT getInverse(){
        return (TemporalRelation_TT)super.getInverse();
    }
}