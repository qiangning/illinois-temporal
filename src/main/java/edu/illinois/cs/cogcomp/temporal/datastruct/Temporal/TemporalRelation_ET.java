package edu.illinois.cs.cogcomp.temporal.datastruct.Temporal;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.configurations.SignalWordSet;
import edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.startTokInSent;

public class TemporalRelation_ET extends TemporalRelation {
    public boolean e_covering_t;
    public String e_covering_t_type;
    /*Features that may not be initialized*/
    private HashSet<String> closestTimexFeats;

    /*Constructors*/
    public TemporalRelation_ET(TemporalNode sourceNode, TemporalNode targetNode, TemporalRelType relType, myTemporalDocument doc) {
        super(sourceNode, targetNode, relType, doc);
        if(sourceNode.getClass().equals(targetNode.getClass())){
            System.out.println("[ERROR] ET is assigned with two same type of nodes.");
            System.exit(-1);
        }
        checkSRLCovering();
    }
    public TemporalRelation_ET(TemporalRelation_ET other, myTemporalDocument doc){
        this(other.getSourceNode(),other.getTargetNode(),other.getRelType(),doc);
    }

    private void checkSRLCovering(){
        EventTemporalNode event = getEventNode();
        TimexTemporalNode timex = getTimexNode();
        e_covering_t = false;
        e_covering_t_type  = "N/A";
        if(getTimexNode().isDCT())
            return;
        List<Pair<String,Constituent>> allSRL = timex.getVerb_srl_covering();
        for(Pair p:allSRL){
            if(p.getSecond()==event.getVerb_srl()) {
                e_covering_t = true;
                e_covering_t_type = (String) p.getFirst();
            }
        }
    }

    /*Feature extraction*/
    @Override
    public void extractAllFeats(){
        feat_extraction_done = true;
        extractSignalWords();
        extractClosestTimexFeats();
    }

    private void extractSignalWords(){
        if(signals_before!=null&&signals_between!=null&&signals_after!=null)
            return;
        signals_before = new HashSet<>();
        signals_between = new HashSet<>();
        signals_after = new HashSet<>();
        if(getTimexNode().isDCT()) return;
        EventTemporalNode sourceEvent = getEventNode();
        TimexTemporalNode targetTimex = getTimexNode();
        TextAnnotation ta = sourceEvent.getTa();// assume targetTimex.getTa() is the same
        int start = startTokInSent(ta,Math.min(sourceEvent.getSentId(),targetTimex.getSentId()));
        int end = endTokInSent(ta,Math.max(sourceEvent.getSentId(),targetTimex.getSentId()));
        int tokId_min = isEventFirstInText()?sourceEvent.getTokenId():targetTimex.getTokenSpan().getFirst();
        int tokId_max = isEventFirstInText()?targetTimex.getTokenSpan().getSecond()-1:sourceEvent.getTokenId();
        int tokId_middle = isEventFirstInText()?targetTimex.getTokenSpan().getFirst():targetTimex.getTokenSpan().getSecond()-1;

        signals_before = getSignalsFromTextSpan(start,tokId_min-1,SignalWordSet.getInstance());
        signals_between = isEventFirstInText()?
                getSignalsFromTextSpan(tokId_min+1,tokId_middle-1,SignalWordSet.getInstance()):
                getSignalsFromTextSpan(tokId_middle+1,tokId_max-1,SignalWordSet.getInstance());
        signals_after = getSignalsFromTextSpan(tokId_max+1,end,SignalWordSet.getInstance());
        signals_before.addAll(getSignalsFromLemmaSpan(start,tokId_min-1,SignalWordSet.getInstance()));
        signals_between.addAll(isEventFirstInText()?
                getSignalsFromLemmaSpan(tokId_min+1,tokId_max-1,SignalWordSet.getInstance()):
                getSignalsFromLemmaSpan(tokId_middle+1,tokId_max-1,SignalWordSet.getInstance()));
        signals_after.addAll(getSignalsFromLemmaSpan(tokId_max+1,end,SignalWordSet.getInstance()));
    }

    private void extractClosestTimexFeats(){
        if(closestTimexFeats!=null)
            return;
        EventTemporalNode e = getEventNode();
        closestTimexFeats = new HashSet<>();
        closestTimexFeats.addAll(e.extractClosestTimexFeats_individual("EVENT"));
    }

    /*Getters and Setters*/

    public EventTemporalNode getEventNode(){
        if(getSourceNode() instanceof EventTemporalNode)
            return (EventTemporalNode) getSourceNode();
        return (EventTemporalNode) getTargetNode();
    }

    public TimexTemporalNode getTimexNode(){
        if(getSourceNode() instanceof TimexTemporalNode)
            return (TimexTemporalNode) getSourceNode();
        return (TimexTemporalNode) getTargetNode();
    }

    public boolean isEventFirstInText(){
        int e_token = getEventNode().getTokenId();
        IntPair t_tokenSpan = getTimexNode().getTokenSpan();
        return e_token < t_tokenSpan.getFirst();
    }
    public boolean isEventFirstInPair(){
        return getSourceNode() instanceof EventTemporalNode;
    }
    public int getETDistance(){
        int e_token = getEventNode().getTokenId();
        IntPair t_tokenSpan = getTimexNode().getTokenSpan();
        boolean e_first = isEventFirstInText();
        if(e_first)
            return e_token - t_tokenSpan.getFirst();
        return e_token - t_tokenSpan.getSecond();
    }

    public HashSet<String> getClosestTimexFeats() {
        return closestTimexFeats;
    }

    @Override
    public TemporalRelation_ET inverse(){
        TemporalRelation_ET ret = new TemporalRelation_ET(getTargetNode(),getSourceNode(),getRelType().inverse(),getDoc());
        // since the source & target have been flipped, we need to reverse the scores in reltype
        ret.getRelType().reverseScores();
        return ret;
    }

    @Override
    @NotNull
    public TemporalRelation_ET getInverse(){
        return (TemporalRelation_ET)super.getInverse();
    }
}
