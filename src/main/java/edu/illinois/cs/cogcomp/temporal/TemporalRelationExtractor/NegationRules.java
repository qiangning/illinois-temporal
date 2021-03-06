package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TreeView;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.eventIndex2TokId;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.endTokInSent;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrieveLemmaAtTokenId;

public class NegationRules {
    int window = 3;
    List<EventTokenCandidate> trainingStructs, testStructs;
    public static String NEGATION = "negation";
    public static String NOT_NEGATION = "not_sure";
    static Set<String> negativeVerbs = new HashSet<String>(){{add("fail");add("oppose");add("decline");add("disavow");add("disagree");add("deny");add("prevent");add("stop");}};
    public void load(){
        try {
            ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
            List<TemporalDocument> allTrainingDocs = TempEval3Reader.deserialize(rm.getString("TimeBank_Ser"));
            allTrainingDocs.addAll(TempEval3Reader.deserialize(rm.getString("AQUAINT_Ser")));
            List<TemporalDocument> allTestingDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
            HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));// docid-->eventid-->axis_label
            trainingStructs = preprocess(allTrainingDocs,axisMap,window);
            testStructs = preprocess(allTestingDocs,axisMap,window);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private List<EventTokenCandidate> preprocess(List<TemporalDocument> docList, HashMap<String,HashMap<Integer,String>> axisMap, int window){
        // axisMap: docid-->index in doc-->raw axis name from CF
        HashMap<String,HashMap<Integer,String>> newAxisMap = new HashMap<>();
        // convert eventid in axisMap to tokenId
        for(int i=0;i<docList.size();i++){
            String docid = docList.get(i).getDocID();
            if(!axisMap.containsKey(docid)) continue;
            HashMap<Integer,Integer> index2TokId = eventIndex2TokId(docList.get(i));
            HashMap<Integer,String> tmpMap = axisMap.get(docid);
            HashMap<Integer,String> tmpMap2 = new HashMap<>();
            for(int eventid:tmpMap.keySet()){
                tmpMap2.put(index2TokId.get(eventid),tmpMap.get(eventid));
            }
            newAxisMap.put(docid,tmpMap2);
        }
        List<EventTokenCandidate> ret = new ArrayList<>();
        for(int i=0;i<docList.size();i++){
            if(!newAxisMap.containsKey(docList.get(i).getDocID())) continue;
            myTemporalDocument doc = new myTemporalDocument(docList.get(i),1);
            ret.addAll(doc.generateAllEventTokenCandidates(window,newAxisMap.get(doc.getDocid())));
        }
        return ret;
    }
    private boolean compare(String gold,String pred){
        return pred.equals(NEGATION)&&gold.equals("no_its_negation")
                ||pred.equals(NOT_NEGATION)&&!gold.equals("no_its_negation");
    }
    public static String negationRule(TextAnnotation ta, int tokenId){
        TreeView dep = (TreeView) ta.getView(ViewNames.DEPENDENCY);
        //TreeView dep = (TreeView) ta.getView(ViewNames.DEPENDENCY_STANFORD);
        boolean until = false;
        for(int i=tokenId+1;i<=endTokInSent(ta,ta.getSentenceId(tokenId));i++){
            if(ta.getToken(i).equals("until")) {
                until = true;
                break;
            }
        }
        if(ta.getToken(tokenId+1).equals("no")
                ||ta.getToken(tokenId+1).equals("not")
                ||ta.getToken(tokenId+1).equals("nothing")
                ||ta.getToken(tokenId+1).equals("nobody")
                ||ta.getToken(tokenId+1).equals("nowhere"))
            return NEGATION;
        if(ta.getToken(tokenId-1).equals("cannot"))
            return NEGATION;
        List<Constituent> clist = dep.getConstituentsCoveringToken(tokenId);
        for(Constituent c:clist){
            List<Relation> rels = c.getOutgoingRelations();
            for(Relation r:rels){
                if(r.getRelationName().equals("neg")&&!until) {
                    return NEGATION;
                }
                if(r.getRelationName().equals("nsubj")){
                    List<Relation> subjrels = r.getTarget().getOutgoingRelations();
                    for(Relation r2:subjrels){
                        if(r2.getRelationName().equals("det")&&r2.getTarget().toString().toLowerCase().equals("no"))
                            return NEGATION;
                    }
                }
            }
            rels = c.getIncomingRelations();
            for(Relation r:rels){
                if(r.getRelationName().equals("xcomp")){
                    String sourceLemma = retrieveLemmaAtTokenId(ta,r.getSource().getStartSpan());
                    if(negativeVerbs.contains(sourceLemma))
                        return NEGATION;
                }
            }
        }
        return NOT_NEGATION;
    }
    public static String negationRule(EventTokenCandidate etc){
        TextAnnotation ta = etc.getDoc().getTextAnnotation();
        return negationRule(ta,etc.getTokenId());
    }
    public void eval(List<EventTokenCandidate> list,String fullpath) throws Exception{
        PrintStream ps = new PrintStream(new File(fullpath));
        double tp = 0, fp = 0, fn = 0;
        for(EventTokenCandidate etc:list){
            String pred = negationRule(etc);
            String gold = etc.getLabel();
            if(pred.equals(NEGATION)) {
                if (compare(gold, pred))
                    tp++;
                else {
                    if(!gold.equals("null"))
                        fp++;
                }
            }
            else{
                if(!compare(gold,pred))
                    fn++;
            }
            if(!compare(gold,pred)){
                ps.println("<hr>");
                ps.println(etc.htmlVisualize());
                ps.printf("<p><span style='color:blue;'><strong>%s</strong></span></p>\n</hr>\n",pred);
            }
        }
        ps.close();
        double tn = list.size()-fp-fn-tp;
        double prec = tp/(tp+fp), rec = tp/(tp+fn);
        double f1 = 2*prec*rec/(prec+rec);
        System.out.printf("Prec: %.0f/%.0f=%4.2f\n",tp,tp+fp,prec*100);
        System.out.printf("Rec: %.0f/%.0f=%4.2f\n",tp,tp+fn,rec*100);
        System.out.printf("F1: %4.2f\n",f1*100);
        System.out.printf("Acc: %.0f/%d=%4.2f\n",tp+tn,list.size(),(tp+tn)*100/list.size());
    }
    public void test() throws Exception{
        System.out.println("Training structs");
        eval(trainingStructs,"data/RuleResult/negation-train.html");
        System.out.println("Test structs");
        eval(testStructs,"data/RuleResult/negation-test.html");
    }
    public static void main(String[] args) throws Exception{
        NegationRules negationRules = new NegationRules();
        negationRules.load();
        negationRules.test();
    }
}
