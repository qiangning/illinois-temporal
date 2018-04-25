package edu.illinois.cs.cogcomp.temporal.datastruct.GeneralGraph;

public abstract class AugmentedNode {
    private int id;
    private String nodeType;
    private String text;

    /*Constructors*/

    public AugmentedNode(int nodeId, String nodeType, String text) {
        this.id = nodeId;
        this.nodeType = nodeType;
        this.text = text;
    }

    /*Functions*/
    public boolean isEqual(AugmentedNode other){
        return this.equals(other)||
                other!=null&&getUniqueId().equals(other.getUniqueId());
    }

    /*Getters and Setters*/

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUniqueId(){return nodeType+":"+id;}
}
