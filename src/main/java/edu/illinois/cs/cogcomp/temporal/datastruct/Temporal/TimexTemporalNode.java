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
    private IntPair tokenSpan;
    private boolean isDCT;
    private String type;
    private String mod;
    private String normVal;
    private int index_in_doc;
    private String pp_head;
    private List<Pair<String, Constituent>> verb_srl_covering = new ArrayList<>();

    /*Features that may not be initialized*/
    private int window;
    private String[] pos_window;
    private String[] lemma_window;

    /*Constructors*/
    public TimexTemporalNode(TimexTemporalNode other){
        this(other.nodeId,other.nodeType,other.text,other.index_in_doc,other.tokenSpan,other.sentId,other.isDCT,other.type,other.mod,other.normVal,other.ta);
    }
    public TimexTemporalNode(int nodeId, String nodeType, String text, int index_in_doc, IntPair tokenSpan, int sentId, boolean isDCT, String type, String mod, String normVal, TextAnnotation ta) {
        super(nodeId, nodeType, text, sentId);
        this.index_in_doc = index_in_doc;
        this.tokenSpan = tokenSpan;
        this.isDCT = isDCT;
        this.type = type;
        this.mod = mod;
        this.normVal = normVal;
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

    public TemporalRelType compareTo(TimexTemporalNode other, TimexTemporalNode dctTimex){// todo now only gives out before/after relations. other relations are all changed to vague.
        if(!getType().equals("DATE") || !other.getType().equals("DATE"))
            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
        String timex1 = getNormVal();
        String timex2 = other.getNormVal();
        /*If both are PRESENT_REF/PAST_REF/FUTURE_REF*/
        if((timex1.equals("PRESENT_REF")||timex1.equals("PAST_REF")||timex1.equals("FUTURE_REF"))
                &&(timex2.equals("PRESENT_REF")||timex2.equals("PAST_REF")||timex2.equals("FUTURE_REF"))){
            switch(timex1){
                case "PRESENT_REF":
                    switch(timex2){
                        case "PAST_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.AFTER);
                        case "FUTURE_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.BEFORE);
                        case "PRESENT_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                        default:
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                    }
                case "PAST_REF":
                    switch(timex2){
                        case "PAST_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                        case "FUTURE_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.BEFORE);
                        case "PRESENT_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.BEFORE);
                        default:
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                    }
                case "FUTURE_REF":
                    switch(timex2){
                        case "PAST_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.AFTER);
                        case "FUTURE_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                        case "PRESENT_REF":
                            return new TemporalRelType(TemporalRelType.relTypes.AFTER);
                        default:
                            return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                    }
                default:
                    return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
            }
        }
        /*If either one is PRESENT_REF/PAST_REF/FUTURE_REF*/
        String dct = dctTimex.getNormVal();
        if(timex1.equals("PRESENT_REF")||timex1.equals("PAST_REF")||timex1.equals("FUTURE_REF")) {
            TemporalRelType comp = mySimpleDate.compareString(dct,timex2);
            switch(timex1){
                case "PRESENT_REF":
                    return comp;
                case "PAST_REF"://timex1 before dct
                    if(comp.getReltype()== TemporalRelType.relTypes.BEFORE)//dct before/equal to/includes timex2
                        return comp;
                    else
                        return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                case "FUTURE_REF"://timex1 after dct
                    if(comp.getReltype()== TemporalRelType.relTypes.AFTER)//dct after/equal to/includes timex2
                        return comp;
                    else
                        return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                default:
                    return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
            }
        }
        if(timex2.equals("PRESENT_REF")||timex2.equals("PAST_REF")||timex2.equals("FUTURE_REF")){
            TemporalRelType comp = mySimpleDate.compareString(timex1,dct);
            switch(timex2){
                case "PRESENT_REF"://timex2 is dct
                    return comp;
                case "PAST_REF"://timex2 before dct
                    if(comp.getReltype()== TemporalRelType.relTypes.AFTER)//timex1 after/equal to/includes dct
                        return comp;
                    else
                        return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                case "FUTURE_REF"://timex2 after dct
                    if(comp.getReltype()== TemporalRelType.relTypes.BEFORE)//timex1 before/equal to/includes dct
                        return comp;
                    else
                        return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                default:
                    return new TemporalRelType(TemporalRelType.relTypes.VAGUE);
            }
        }
		/*both are standard dates*/
        return mySimpleDate.compareString(timex1,timex2);
    }

    /*Getters and Setters*/

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

    @Override
    public String toString() {
        return "TimexTemporalNode{" +
                "isDCT=" + isDCT +
                ", nodeId=" + nodeId +
                ", sentId=" + sentId +
                ", tokenSpan=" + tokenSpan +
                ", text=" + getText()+
                ", type='" + type + '\'' +
                ", mod='" + mod + '\'' +
                ", normVal='" + normVal + '\'' +
                '}';
    }


}
