package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedGraph;

import java.util.ArrayList;
import java.util.List;

public class TemporalGraph extends AugmentedGraph<TemporalNode,TemporalRelation> {
    /*Variables*/
    /*Constructors*/
    public TemporalGraph() {super();}

    public TemporalGraph(List<TemporalNode> nodeMap, List<TemporalRelation> relations) {
        super(nodeMap, relations);
    }
    /*Functions*/

    // TO-DO: get EE / ET / TT sub-graphs

    public List<TemporalRelation_EE> getAllEERelations(int sentDiff){
        // All EE
        // - source should be before target
        // - only sentDiff can be kept
        List<TemporalRelation> allRelations = getRelations();
        List<TemporalRelation_EE> allEERelations = new ArrayList<>();
        for(TemporalRelation rel:allRelations){
            if(rel.getSourceNode() instanceof EventTemporalNode
                    && rel.getTargetNode() instanceof EventTemporalNode){
                TemporalRelation_EE ee_rel = (TemporalRelation_EE) rel;
                if(ee_rel.getSourceEvent().getTokenId()<ee_rel.getTargetEvent().getTokenId()&&
                        (sentDiff<0||sentDiff==ee_rel.getSentDiff()))
                    allEERelations.add(ee_rel);
            }
        }
        return allEERelations;
    }
}
