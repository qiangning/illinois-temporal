package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelation;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;

/**
 * Created by chuchu on 12/20/17.
 */
public class TemporalRelation extends BinaryRelation<TemporalNode> {
    private static int LabelMode;
    private myTemporalDocument doc;

    /*Constructors*/

    public TemporalRelation(TemporalNode sourceNode, TemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType);
        this.doc = doc;
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
        return new TemporalRelation(getTargetNode(),getSourceNode(),(TemporalRelType) getRelType().inverse(),doc);
    }

    public myTemporalDocument getDoc() {
        return doc;
    }

    public String getLabel(){
        String label = getRelType().getReltype().getName();
        temprelAnnotationReader.Q1_Q2_temprel tuple = new temprelAnnotationReader.Q1_Q2_temprel(new TemporalRelType(label));
        switch (LabelMode){
            case 0:
                return label;
            case 1:
                return tuple.isQ1()?"q1:yes":"q1:no";
            case 2:
                return tuple.isQ2()?"q2:yes":"q2:no";
            default:
                return label;
        }
    }

    @Override
    public String toString() {
        return String.format("%s-->%s: %s",getSourceNode().getUniqueId(),getTargetNode().getUniqueId(), getRelType().toString());
    }

    public static void setLabelMode(int labelMode) {
        TemporalRelation.LabelMode = labelMode;
    }
}
