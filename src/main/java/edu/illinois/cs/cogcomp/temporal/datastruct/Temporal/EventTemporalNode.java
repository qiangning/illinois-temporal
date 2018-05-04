package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.PredicateArgumentView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventTokenCandidate;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.configurations.VerbIgnoreSet;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.EventNodeType;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.*;

public class EventTemporalNode extends TemporalNode{
    private String pos;
    private String lemma;
    private String sense;
    private String cluster;// to-do: frame cluster, or custom cluster
    private int eid;
    private int eiid;
    private int index_in_doc;
    private int tokenId;
    private String pp_head;//prepositional phrase head
    private boolean isReporting,isIntention;
    private TimexTemporalNode closestTimex_left,closestTimex_right;
    private Constituent verb_srl;
    private List<Constituent> verb_srl_same_sentence = new ArrayList<>();
    private List<Pair<String, Constituent>> verb_srl_covering = new ArrayList<>();

    private myTemporalDocument doc;

    /*Features that may not be initialized*/
    private static WNSim wnsim;
    private List<String> synsets;
    private int window;
    private String[] pos_window;
    private String[] lemma_window;

    public EventTemporalNode(int nodeId, String nodeType, String text, int eid, int eiid, int index_in_doc,int tokenId, TextAnnotation ta, myTemporalDocument doc) {
        super(nodeId, nodeType, text, ta.getSentenceId(tokenId));
        this.eid = eid;
        this.eiid = eiid;
        this.index_in_doc = index_in_doc;
        this.tokenId = tokenId;
        this.ta = ta;
        this.doc = doc;
        pos = retrievePOSAtTokenId(ta,tokenId);
        lemma = retrieveLemmaAtTokenId(ta,tokenId);
        pp_head = retrievePPHeadOfTokenId(ta,tokenId);
        sense = "01";

        isReporting = SignalWordSet.getInstance().reportingVerbSet.contains(lemma);
        isIntention = SignalWordSet.getInstance().intentionVerbSet.contains(lemma);

        // closest Timex
        int i=0;
        List<TimexTemporalNode> allTimexes = doc.getTimexList();
        while(i<allTimexes.size() && allTimexes.get(i).getTokenSpan().getSecond()<tokenId){
            closestTimex_left = allTimexes.get(i);
            i++;
        }
        if(i<allTimexes.size())
            closestTimex_right = allTimexes.get(i);

        // Verb SRL from the same sentence
        List<Constituent> allPredicates = ((PredicateArgumentView)doc.getTextAnnotation().getView(ViewNames.SRL_VERB)).getPredicates();
        for(Constituent c:allPredicates){
            if(sentId==c.getSentenceId()&& !VerbIgnoreSet.getInstance().srlVerbIgnoreSet.contains(c.getAttribute("predicate"))) {
                verb_srl_same_sentence.add(c);
                if(c.getStartSpan()==tokenId) {
                    verb_srl = c;
                    sense = verb_srl.getAttribute("SenseNumber");
                }
                List<Relation> tmp = c.getOutgoingRelations();
                for(Relation r:tmp){
                    if(r.getTarget().doesConstituentCover(tokenId))
                        verb_srl_covering.add(new Pair<>(r.getRelationName(),c));
                }
            }
        }
        cluster = lemma +"."+ sense;
    }

    public EventTemporalNode(EventTokenCandidate etc, int nodeId,String text, int eid, int eiid, int index_in_doc){
        super(nodeId, EventNodeType,text, etc.getSentId());
        this.eid = eid;
        this.eiid = eiid;
        this.index_in_doc = index_in_doc;
        this.tokenId = etc.getTokenId();
        this.doc = etc.getDoc();
        this.ta = doc.getTextAnnotation();
        pos = etc.getPos();
        lemma = etc.getLemma();
        pp_head = etc.getPp_head();
        isReporting = etc.isReporting();
        isIntention = etc.isIntention();
        closestTimex_left = etc.getClosestTimex_left();
        closestTimex_right = etc.getClosestTimex_right();
        verb_srl = etc.getVerb_srl();
        verb_srl_same_sentence = etc.getVerb_srl_same_sentence();
        verb_srl_covering = etc.getVerb_srl_covering();
        sense = verb_srl!=null?verb_srl.getAttribute("SenseNumber"):"01";
        cluster = lemma +"."+ sense;
    }

    /*Feature extraction*/
    public void extractSynsets(){
        synsets = retrieveSynsetUsingLemmaAndPos(getWNsim(),lemma,pos);
    }

    public void extractPosLemmaWin(int win){
        this.window = win;
        pos_window = retrievePOSWindow(ta,tokenId,win);
        lemma_window = retrieveLemmaWindow(ta,tokenId,win);
    }

    /*Getters and setters*/
    public int getTokenId() {
        return tokenId;
    }

    public int getEiid() {
        return eiid;
    }

    public int getIndex_in_doc() {
        return index_in_doc;
    }

    public String getPp_head() {
        return pp_head;
    }

    public String getPos() {
        return pos;
    }

    public String getLemma() {
        return lemma;
    }

    public List<String> getSynsets() {
        return synsets;
    }

    public void setSynsets(List<String> synsets) {
        this.synsets = synsets;
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

    public String getCluster() {
        return cluster;
    }

    public boolean isReporting() {
        return isReporting;
    }

    public boolean isIntention() {
        return isIntention;
    }

    public TimexTemporalNode getClosestTimex_left() {
        return closestTimex_left;
    }

    public TimexTemporalNode getClosestTimex_right() {
        return closestTimex_right;
    }

    public Constituent getVerb_srl() {
        return verb_srl;
    }

    public List<Constituent> getVerb_srl_same_sentence() {
        return verb_srl_same_sentence;
    }

    public List<Pair<String, Constituent>> getVerb_srl_covering() {
        return verb_srl_covering;
    }

    public myTemporalDocument getDoc() {
        return doc;
    }

    public static WNSim getWNsim() {
        if(wnsim==null){
            try {
                ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
                WNSim wnsim = WNSim.getInstance(rm.getString("WordNet_Dir"));
                setWNsim(wnsim);
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("WNSim loading error. Exiting now.");
                System.exit(-1);}
        }
        return wnsim;
    }

    public static void setWNsim(WNSim wnsim) {
        EventTemporalNode.wnsim = wnsim;
    }

    @Override
    @NotNull
    public String toString() {
        return "EventTemporalNode{" +
                "pos='" + pos + '\'' +
                ", lemma='" + lemma + '\'' +
                ", eiid=" + eiid +
                ", tokenId=" + tokenId +
                ", synsets=" + synsets +
                ", pp_head='" + pp_head + '\'' +
                ", pos_window=" + Arrays.toString(pos_window) +
                ", lemma_window=" + Arrays.toString(lemma_window) +
                '}';
    }
}
