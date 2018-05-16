package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph.AugmentedGraph;
import edu.illinois.cs.cogcomp.temporal.utils.GraphVisualizer.GraphJavaScript;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
        for(TemporalRelation rel:other.getRelations()){
            if(rel instanceof TemporalRelation_EE)
                addRelNoDup(new TemporalRelation_EE((EventTemporalNode)getNode(rel.getSourceNode().getUniqueId()),
                        (EventTemporalNode)getNode(rel.getTargetNode().getUniqueId()),
                        new TemporalRelType(rel.getRelType()),
                        doc));
            else if(rel instanceof TemporalRelation_ET){
                addRelNoDup(new TemporalRelation_ET((EventTemporalNode)getNode(rel.getSourceNode().getUniqueId()),
                        (TimexTemporalNode)getNode(rel.getTargetNode().getUniqueId()),
                        new TemporalRelType(rel.getRelType()),
                        doc));
            }
            else if(rel instanceof TemporalRelation_TT){
                addRelNoDup(new TemporalRelation_TT((TimexTemporalNode)getNode(rel.getSourceNode().getUniqueId()),
                        (TimexTemporalNode)getNode(rel.getTargetNode().getUniqueId()),
                        new TemporalRelType(rel.getRelType()),
                        doc));
            }
            else{
                System.out.println("[WARNING] unexpected type of temporal relations (EE/ET/TT).");
            }
        }
    }

    /*Functions*/

    //todo
    public void dropAllEERelations(){
        // todo
    }

    public void dropAllETRelations(){
        // todo
    }

    public void dropAllTTRelations(){
        // todo
    }

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

    public void visualize(String htmlDir){
        IOUtils.mkdir(htmlDir);
        String fname = htmlDir+ File.separator+doc.getDocid()+".html";
        GraphJavaScript graphJavaScript = new GraphJavaScript(fname);
        for(String nodeid:nodeMap.keySet()){
            TemporalNode node = nodeMap.get(nodeid);
            graphJavaScript.addVertex(nodeid,node.getText());
        }
        graphJavaScript.sortVertexes();
        for(TemporalRelation rel:relations_directed){
            String id1 = rel.getSourceNode().getUniqueId();
            String id2 = rel.getTargetNode().getUniqueId();
            TemporalRelType.relTypes reltype = rel.getRelType().getReltype();
            switch (reltype.getName().toLowerCase()){
                case "before":
                    graphJavaScript.addEdge(id1,id2,"");
                    break;
                case "after":
                    graphJavaScript.addEdge(id2,id1,"");
                    break;
                case "equal":
                    graphJavaScript.addEdge(id1,id2,"");
                    graphJavaScript.addEdge(id2,id1,"");
                    break;
            }
        }
        graphJavaScript.createJS();
    }

    // todo: get ET / TT sub-graphs

    public List<TemporalRelation_EE> getAllEERelations(int sentDiff){
        // All EE
        // - source should be before target
        // - only sentDiff can be kept (sentDiff<0 means this option is inactive)
        List<TemporalRelation> allRelations = getRelations();
        List<TemporalRelation_EE> allEERelations = new ArrayList<>();
        for(TemporalRelation rel:allRelations){
            if(rel.getSourceNode() instanceof EventTemporalNode
                    && rel.getTargetNode() instanceof EventTemporalNode){
                TemporalRelation_EE ee_rel = (TemporalRelation_EE) rel;
                if(ee_rel.getSourceNode().getTokenId()<ee_rel.getTargetNode().getTokenId()&&
                        (sentDiff<0||sentDiff==ee_rel.getSentDiff()))
                    allEERelations.add(ee_rel);
            }
        }
        return allEERelations;
    }

    public List<TemporalRelation_ET> getAllETRelations(int sentDiff){
        // All ET
        // - E should be T in pair
        // - only sentDiff can be kept (sentDiff=-1 means E-DCT;sentDiff=-2 means everything)
        List<TemporalRelation> allRelations = getRelations();
        List<TemporalRelation_ET> allETRelations = new ArrayList<>();
        for(TemporalRelation rel:allRelations){
            if(rel.getSourceNode() instanceof EventTemporalNode
                    && rel.getTargetNode() instanceof TimexTemporalNode){
                TemporalRelation_ET et_rel = (TemporalRelation_ET) rel;
                if(sentDiff==-2
                        ||sentDiff==-1&&et_rel.getTimexNode().isDCT()
                        ||sentDiff==et_rel.getSentDiff()){
                    allETRelations.add(et_rel);
                }
            }
        }
        return allETRelations;
    }
}
