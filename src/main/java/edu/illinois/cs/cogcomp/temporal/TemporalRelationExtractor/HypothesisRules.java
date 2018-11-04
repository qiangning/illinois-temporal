package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.NegationRules.negationRule;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.eventIndex2TokId;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;

public class HypothesisRules {
    int window = 3;
    List<EventTokenCandidate> trainingStructs, testStructs;
    public static String HYPOTHESIS = "hypothesis";
    static String NOT_HYPOTHESIS = "not_sure";
    static String[] keys = new String[]{"if","unless"};
    static NegationRules negationRules = new NegationRules();
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
        return pred.equals(HYPOTHESIS)&&gold.equals("no_its_hypotheticalcondition")
                ||pred.equals(NOT_HYPOTHESIS)&&!gold.equals("no_its_hypotheticalcondition");
    }
    private static List<Constituent> targetConstituentOfRelation(Constituent c, String targetRelationName){
        List<Constituent> ret = new ArrayList<>();
        if(c!=null) {
            List<Relation> relations = c.getOutgoingRelations();
            for (Relation r : relations) {
                if (r.getRelationName().toLowerCase().equals(targetRelationName.toLowerCase())) {
                    ret.add(r.getTarget());
                }
            }
        }
        return ret;
    }
    private static List<Constituent> sourceConstituentOfRelation(Constituent c, String sourceRelationName){
        List<Constituent> ret = new ArrayList<>();
        if(c!=null) {
            List<Relation> relations = c.getIncomingRelations();
            for (Relation r : relations) {
                if (r.getRelationName().toLowerCase().equals(sourceRelationName.toLowerCase())) {
                    ret.add(r.getSource());
                }
            }
        }
        return ret;
    }
    private static boolean match2strings(Constituent c, String[] keys){
        for(String k:keys){
            if(c.toString().toLowerCase().equals(k.toLowerCase()))
                return true;
        }
        return false;
    }
    private static boolean startwithStrings(Constituent c, String[] keys){
        for(String k:keys){
            if(c.toString().toLowerCase().startsWith(k.toLowerCase()))
                return true;
        }
        return false;
    }
    public static String hypothesisRule(EventTokenCandidate etc){
        TextAnnotation ta = etc.getDoc().getTextAnnotation();
        TreeView dep = (TreeView) ta.getView(ViewNames.DEPENDENCY);
        // rule1: v->advcl->v2 AND v2->mark->"if"/"unless" AND v->AM-ADV->a constituent starting with "if"/"unless"
        if(negationRule(etc).equals(NegationRules.NEGATION))
            return NOT_HYPOTHESIS;
        boolean b1 = false, b2 = false; //b1 for dependency parsing rule, and b2 for SRL rule
        List<Constituent> clist = dep.getConstituentsCoveringToken(etc.getTokenId());
        for(Constituent c:clist){
            List<Constituent> advclConsts = targetConstituentOfRelation(c,"advcl");
            for(Constituent c2:advclConsts){
                List<Constituent> markConsts = targetConstituentOfRelation(c2,"mark");
                for(Constituent c3:markConsts){
                    if(match2strings(c3,keys)) {
                        b1 = true;
                        break;
                    }
                }
                if(b1) break;
            }
            if(b1) break;
        }
        List<Constituent> consts = targetConstituentOfRelation(etc.getVerb_srl(),"AM-ADV");
        //consts.addAll(targetConstituentOfRelation(etc.getVerb_srl(),"AM-TMP"));
        for(Constituent c:consts){
            if(startwithStrings(c,keys)) {
                b2 = true;
                break;
            }
        }

        if(b1)
            return HYPOTHESIS;
        // rule2: some other v2->advcl->v AND v->mark->if/unless AND v appears in the AM-ADV constituent of another v2, while the consituent starts by if/unless
        b1 = false;
        b2 = false;
        for(Constituent c:clist){
            List<Constituent> advclConsts = sourceConstituentOfRelation(c,"advcl");
            if(advclConsts.size()>0) {
                // check if any advclConsts is negation
                boolean b3 = false;
                for(Constituent c3:advclConsts){
                    if(negationRule(ta, c3.getStartSpan()).equals(NegationRules.NEGATION)) {
                        b3 = true;
                        break;
                    }
                }
                if(b3) break;

                List<Constituent> markConsts = targetConstituentOfRelation(c,"mark");
                for (Constituent c2 : markConsts) {
                    if (match2strings(c2, keys)) {
                        b1 = true;
                        break;
                    }
                }
            }
            if(b1) break;
        }
        List<Pair<String,Constituent>> list = etc.getVerb_srl_covering();
        for(Pair<String,Constituent> p:list){
            //if((p.getFirst().toLowerCase().equals("am-adv")||p.getFirst().toLowerCase().equals("am-tmp"))
            if((p.getFirst().toLowerCase().equals("am-adv"))
                    &&startwithStrings(p.getSecond(),keys)){
                b2 = true;
                break;
            }
        }
        if(b1)
            return HYPOTHESIS;
        return NOT_HYPOTHESIS;
    }
    public void eval(List<EventTokenCandidate> list,String fullpath) throws Exception{
        PrintStream ps = new PrintStream(new File(fullpath));
        double tp = 0, fp = 0, fn = 0;
        for(EventTokenCandidate etc:list){
            String pred = hypothesisRule(etc);
            String gold = etc.getLabel();
            if(pred.equals(HYPOTHESIS)) {
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
        eval(trainingStructs,"data/RuleResult/hypothesis-train.html");
        System.out.println("Test structs");
        eval(testStructs,"data/RuleResult/hypothesis-test.html");
    }
    public static void main(String[] args) throws Exception{
        HypothesisRules hypothesisRules = new HypothesisRules();

        hypothesisRules.load();
        hypothesisRules.test();
    }
}
