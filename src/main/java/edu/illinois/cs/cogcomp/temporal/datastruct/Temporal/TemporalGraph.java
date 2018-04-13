package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedGraph;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedNode;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.BinaryRelation;

import java.util.HashMap;
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

}
