package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

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

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.eventIndex2TokId;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;

public class IntentionRules {
    int window = 3;
    List<EventTokenCandidate> trainingStructs, testStructs;
    public static String INTENTION = "intention";
    static String NOT_INTENTION = "not_sure";
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
    private List<EventTokenCandidate> preprocess(List<TemporalDocument> docList,HashMap<String,HashMap<Integer,String>> axisMap, int window){
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
    public static String intentionRule_to(EventTokenCandidate etc, int window){
        if(etc.getLemma_window()[window-1].equals("to")) {
            return etc.getLemma_window()[window-2].equals("start")||etc.getLemma_window()[window-2].equals("begin")? NOT_INTENTION:INTENTION;
        }
        else if(etc.getLemma_window()[window-2].equals("to")&&etc.getLemma_window()[window-1].equals("be")&&etc.getPos().equals("VBG"))
            return INTENTION;
        else
            return NOT_INTENTION;
    }
    private boolean compare(String gold,String pred){
        return pred.equals(INTENTION)&&gold.equals("no_its_intentionwishopinion")
                ||pred.equals(NOT_INTENTION)&&!gold.equals("no_its_intentionwishopinion");
    }
    public void eval(List<EventTokenCandidate> list,String fullpath) throws Exception{
        PrintStream ps = new PrintStream(new File(fullpath));
        double tp = 0, fp = 0, fn = 0;
        for(EventTokenCandidate etc:list){
            String pred = intentionRule_to(etc,window);
            String gold = etc.getLabel();
            if(pred.equals(INTENTION)) {
                if (compare(gold, pred))
                    tp++;
                else
                    fp++;
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
        eval(trainingStructs,"data/RuleResult/intention-todo-train.html");
        System.out.println("Test structs");
        eval(testStructs,"data/RuleResult/intention-todo-test.html");
    }
    public static void main(String[] args) throws Exception {
        IntentionRules itr = new IntentionRules();
        itr.load();
        itr.test();
    }
}
