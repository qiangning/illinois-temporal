package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.PredicateArgumentView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.configurations.VerbIgnoreSet;

import java.util.ArrayList;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrieveLemmaWindow_Span;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrievePOSWindow_Span;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrievePPHeadOfTokenId;

public class TimexTemporalNode extends TemporalNode{
    public static final String startNode = "START", endNode = "END";
    private IntPair tokenSpan;
    private boolean isDCT;
    private String type;
    private String mod;
    private String normVal;
    private int index_in_doc;
    private String pp_head;
    private List<Pair<String, Constituent>> verb_srl_covering = new ArrayList<>();
    private boolean isStart;
    //private TimexTemporalNode pairNode; // if this is start, pairNode is end; otherwise, pairNode is start.

    /*Features that may not be initialized*/
    private int window;
    private String[] pos_window;
    private String[] lemma_window;

    /*Constructors*/
    public TimexTemporalNode(TimexTemporalNode other){
        this(other.nodeId,other.nodeType,other.text,other.index_in_doc,other.tokenSpan,other.sentId,other.isDCT,other.isStart,other.type,other.mod,other.normVal,other.ta);
    }
    public TimexTemporalNode(int nodeId, String nodeType, String text, int index_in_doc, IntPair tokenSpan, int sentId, boolean isDCT, boolean isStart, String type, String mod, String normVal, TextAnnotation ta) {
        super(nodeId, nodeType, text, sentId);
        this.index_in_doc = index_in_doc;
        this.tokenSpan = tokenSpan;
        this.isDCT = isDCT;
        this.isStart = isStart;
        this.type = type;
        this.mod = mod;
        this.normVal = normVal;
        /*this.pairNode = pairNode;
        if(pairNode!=null&&(!pairNode.pairNode.isEqual(this)||pairNode.isStart==isStart)){
            System.out.println("[WARNING] pairNode not correct.");
        }*/
        this.ta = ta;
        pp_head = retrievePPHeadOfTokenId(ta,this.tokenSpan.getFirst());
        // Verb SRL from the same sentence
        List<Constituent> allPredicates = ((PredicateArgumentView)ta.getView(ViewNames.SRL_VERB)).getPredicates();
        for(Constituent c:allPredicates){
            if(sentId==c.getSentenceId()&& !VerbIgnoreSet.getInstance().srlVerbIgnoreSet.contains(c.getAttribute("predicate"))) {
                List<Relation> tmp = c.getOutgoingRelations();
                for(Relation r:tmp){
                    if(r.getTarget().doesConstituentCover(tokenSpan.getFirst()))
                        verb_srl_covering.add(new Pair<>(r.getRelationName(),c));
                }
            }
        }
    }

    /*Functions*/
    public void extractAllFeats(int win){
        extractPosLemmaWin(win);
    }
    public void extractPosLemmaWin(int win){
        this.window = win;
        if(pos_window==null||pos_window.length!=win*2+1)
            pos_window = retrievePOSWindow_Span(ta,tokenSpan,win);
        if(lemma_window==null||lemma_window.length!=win*2+1)
            lemma_window = retrieveLemmaWindow_Span(ta,tokenSpan,win);
    }

    /*Getters and Setters*/

    @Override
    public String getNodeType(){
        return nodeType+":"+(isStart?startNode:endNode);
    }

    public IntPair getTokenSpan() {
        return tokenSpan;
    }

    public boolean isDCT() {
        return isDCT;
    }

    public String getType() {
        return type==null?"null":type;
    }

    public String getNormVal() {
        return normVal;
    }

    public int getIndex_in_doc() {
        return index_in_doc;
    }

    public String getMod() {
        return mod==null?"null":mod;
    }

    public int getWindow() {
        return window;
    }

    public String[] getPos_window() {
        return pos_window;
    }

    public String[] getLemma_window() {
        return lemma_window;
    }

    public int getLength(){
        return tokenSpan.getSecond()-tokenSpan.getFirst();
    }

    public String getPp_head() {
        return pp_head;
    }

    public List<Pair<String, Constituent>> getVerb_srl_covering() {
        return verb_srl_covering;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    @Override
    public String toString() {
        return "TimexTemporalNode{" +
                "isDCT=" + isDCT +
                ", sentId=" + sentId +
                ", tokenSpan=" + tokenSpan +
                ", text=" + getText()+
                ", type='" + type + '\'' +
                ", mod='" + mod + '\'' +
                ", normVal='" + normVal + '\'' +
                ", index_in_doc=" + index_in_doc +
                '}';
    }


}
