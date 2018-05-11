package edu.illinois.cs.cogcomp.temporal.utils.GraphVisualizer;

public class vertex {
    private String uniqueid;
    private String text;

    public vertex(String uniqueid, String text) {
        this.uniqueid = uniqueid;
        this.text = text;
    }

    public boolean equals(vertex v2){
        return uniqueid.equals(v2.uniqueid);
    }

    public String getUniqueid() {
        return uniqueid;
    }

    public String getText() {
        return text;
    }

    public String toString(){
        return uniqueid+":"+text;
    }
}
