package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedGraph;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TemporalGraph extends AugmentedGraph<TemporalNode,TemporalRelation>{
    /*Variables*/
    protected myTemporalDocument doc;
    /*Constructors*/
    public TemporalGraph(myTemporalDocument doc) {
        super();
        this.doc = doc;
    }

    public TemporalGraph(List<TemporalNode> nodeMap, List<TemporalRelation> relations) {
        super(nodeMap, relations);
    }

    public TemporalGraph(TemporalGraph other){
        super();
        doc = null;
        for(String str:other.nodeMap.keySet()) {
            TemporalNode node = other.nodeMap.get(str);
            if(node instanceof EventTemporalNode)
                nodeMap.put(str, new EventTemporalNode((EventTemporalNode)node,doc));
            else if(node instanceof TimexTemporalNode)
                nodeMap.put(str, new TimexTemporalNode((TimexTemporalNode)node));
            else{
                System.out.println("[WARNING] unexpected type of nodes (Event/Timex).");
                nodeMap.put(str, new TemporalNode(node));
            }
        }
        for(TemporalRelation rel:other.relations){
            if(rel instanceof TemporalRelation_EE)
                addRelNoDup(new TemporalRelation_EE((TemporalRelation_EE)rel,doc));
            else if(rel instanceof TemporalRelation_ET){
                addRelNoDup(new TemporalRelation_ET((TemporalRelation_ET)rel,doc));
            }
            else if(rel instanceof TemporalRelation_TT){
                addRelNoDup(new TemporalRelation_TT((TemporalRelation_TT)rel,doc));
            }
            else{
                System.out.println("[WARNING] unexpected type of temporal relations (EE/ET/TT).");
            }
        }
    }

    /*Functions*/


    /*Getters and Setters*/
    @Override
    public TemporalRelation getRelBetweenNodes(String uniqueId1, String uniqueId2){
        return super.getRelBetweenNodes(uniqueId1,uniqueId2);
    }

    @Nullable
    public TemporalRelation_EE getEERelBetweenEvents(String uniqueId1, String uniqueId2){
        if(getNode(uniqueId1) instanceof TimexTemporalNode || getNode(uniqueId2) instanceof TimexTemporalNode)
            return null;
        TemporalRelation rel = getRelBetweenNodes(uniqueId1,uniqueId2);
        if(rel==null||rel.isNull())
            return null;
        if(rel instanceof TemporalRelation_EE)
            return (TemporalRelation_EE)rel;
        System.out.println("[WARNING] Get EE Relation Unexpectedly Failed. Null is returned.");
        return null;
    }

    public boolean setRelBetweenNodes(String uniqueId1, String uniqueId2, TemporalRelType relType){
        if(relType==null||relType.isNull()) {
            dropRelation(uniqueId1, uniqueId2);
            return true;
        }
        TemporalRelation temporalRelation = getRelBetweenNodes(uniqueId1,uniqueId2);
        if(temporalRelation==null) {
            TemporalNode n1 = getNode(uniqueId1);
            TemporalNode n2 = getNode(uniqueId2);
            if(n1==null||n2==null)
                return false;
            if(n1 instanceof EventTemporalNode && n2 instanceof EventTemporalNode){
                temporalRelation = new TemporalRelation_EE((EventTemporalNode)n1,(EventTemporalNode)n2,relType,doc);
                addRelNoDup(temporalRelation);
            }
            else if(n1 instanceof TimexTemporalNode && n2 instanceof TimexTemporalNode){
                //todo TT
            }
            else{
                //todo ET
            }
        }
        else
            temporalRelation.setRelType(relType);
        return true;
    }

    // TO-DO: get ET / TT sub-graphs

    public List<TemporalRelation_EE> getAllEERelations(int sentDiff){
        // All EE
        // - source should be before target
        // - only sentDiff can be kept (sentDiff<0 means this option is inactive)
        List<TemporalRelation> allRelations = getRelations();
        List<TemporalRelation_EE> allEERelations = new ArrayList<>();
        for(TemporalRelation rel:allRelations){
            if(rel.getSourceNode() instanceof EventTemporalNode
                    && rel.getTargetNode() instanceof EventTemporalNode
                    || rel instanceof TemporalRelation_EE){
                TemporalRelation_EE ee_rel = (TemporalRelation_EE) rel;
                if(ee_rel.getSourceNode().getTokenId()<ee_rel.getTargetNode().getTokenId()&&
                        (sentDiff<0||sentDiff==ee_rel.getSentDiff()))
                    allEERelations.add(ee_rel);
            }
        }
        return allEERelations;
    }
}
