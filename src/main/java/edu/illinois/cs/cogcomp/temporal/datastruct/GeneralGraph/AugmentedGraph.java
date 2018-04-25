package edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chuchu on 12/20/17.
 */
public class AugmentedGraph<Node extends AugmentedNode, Relation extends BinaryRelation<Node>>  {
    private HashMap<String,Node> nodeMap;
    private List<Relation> relations;
    private HashMap<String,List<Relation>> nodeInRelationMap, nodeOutRelationMap;

    // TO-DO: graph closure using transitivity triplets defined in BinaryRelation

    /*Constructors*/

    public AugmentedGraph() {
        this.nodeMap = new HashMap<>();
        this.relations = new ArrayList<>();
        nodeInRelationMap = new HashMap<>();
        nodeOutRelationMap = new HashMap<>();
    }

    public AugmentedGraph(List<Node> nodeMap, List<Relation> relations) {
        this.nodeMap = new HashMap<>();
        for(Node node:nodeMap) {
            int ret = addNodeNoDup(node);
            if(ret!=1){
                System.out.printf("[WARNING]: node %s is invalid or exists duplicates in nodeMap input to AugmentedGraph\n",node.getUniqueId());
                System.out.printf("[ERROR CODE]: %d\n",ret);
                System.out.println(node.toString());
            }
        }

        this.relations = new ArrayList<>();
        for(Relation rel : relations){
            int ret = addRelNoDup(rel);
            if(ret!=1){
                System.out.println("[WARNING]: add relation failed.");
                System.out.printf("[ERROR CODE]: %d\n",ret);
                System.out.println(rel.toString());
            }
        }
    }

    /*Functions*/
    public int addNodeNoDup(Node newnode){
        if(newnode==null){// add null
            return -1;
        }
        if(nodeMap.containsKey(newnode.getUniqueId())){//duplicate
            return 0;
        }
        nodeMap.put(newnode.getUniqueId(),newnode); //success
        return 1;
    }
    public int addRelNoDup(Relation newrel){
        if(newrel==null||newrel.isNull()){//invalid newrel
            return -1;
        }
        Node sourceNode = getNode(newrel.getSourceNode().getUniqueId());
        Node targetNode = getNode(newrel.getTargetNode().getUniqueId());
        if(sourceNode==null||targetNode==null){// newrel contains nodes that don't exist in nodeMap.
            return -2;
        }
        BinaryRelation<Node> rel = getRelBetweenNodes(sourceNode.getUniqueId(),targetNode.getUniqueId());
        if(rel!=null&&!rel.isNull()){// newrel already exists in graph
            return 0;
        }
        // success
        addNewRelation(newrel);
        return 1;
    }

    public boolean dropNode(String nodeUniqueId){
        // nodeMap
        if(!nodeMap.containsKey(nodeUniqueId))
            return false;
        nodeMap.remove(nodeUniqueId);
        // relations
        List<Relation> newRelations = new ArrayList<>();
        for(Relation rel:relations){
            if(rel.getSourceNode().getUniqueId().equals(nodeUniqueId)
                    ||rel.getTargetNode().getUniqueId().equals(nodeUniqueId))
                continue;
            newRelations.add(rel);
        }
        relations = newRelations;
        // nodeOutRelationMap and nodeInRelationMap
        nodeInRelationMap.remove(nodeUniqueId);
        nodeOutRelationMap.remove(nodeUniqueId);
        return true;
    }

    public void dropAllNodes(){
        nodeMap = new HashMap<>();
        dropAllRelations();
    }

    private void addNewRelation(Relation rel){
        relations.add(rel);
        relations.add((Relation)rel.getInverse());
        String uniqueId1 = rel.getSourceNode().getUniqueId();
        String uniqueId2 = rel.getTargetNode().getUniqueId();
        addRelationToMap(uniqueId1,rel,nodeOutRelationMap);
        addRelationToMap(uniqueId2,rel,nodeInRelationMap);
        addRelationToMap(uniqueId1,(Relation)rel.getInverse(),nodeInRelationMap);
        addRelationToMap(uniqueId2,(Relation)rel.getInverse(),nodeOutRelationMap);
    }

    private void addRelationToMap(String uniqueId, Relation rel, HashMap<String,List<Relation>> map){
        if(!map.containsKey(uniqueId))
            map.put(uniqueId,new ArrayList<>());
        map.get(uniqueId).add(rel);
    }

    public void dropAllRelations(){
        relations = new ArrayList<>();
        nodeInRelationMap = new HashMap<>();
        nodeOutRelationMap = new HashMap<>();
    }

    /*Getters and Setters*/

    @Nullable
    public Node getNode(String uniqueId){
        return nodeMap.getOrDefault(uniqueId,null);
    }
    @Nullable
    public Relation getRelBetweenNodes(String uniqueId1, String uniqueId2){
        List<Relation> id1_outmap = nodeOutRelationMap.getOrDefault(uniqueId1, new ArrayList<>());
        for(Relation rel:id1_outmap){
            if(rel.getTargetNode().getUniqueId().equals(uniqueId2)){
                return rel;
            }
        }
        return null;
    }

    public List<Relation> getRelations() {
        return relations;
    }
}
