package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.EventTemporalNode;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.lbjava.EventDetector.eventDetector;
import edu.illinois.cs.cogcomp.temporal.lbjava.TempRelCls.eeTempRelCls;
import edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader;
import edu.illinois.cs.cogcomp.temporal.utils.WordNet.WNSim;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument.EventNodeType;
import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class TempRelAnnotator {
    private myTemporalDocument doc;
    private EventAxisLabeler axisLabeler;
    private TempRelLabeler tempRelLabeler;
    private ResourceManager rm;

    public TempRelAnnotator(myTemporalDocument doc) {
        this.doc = doc;
        try {
            rm = new temporalConfigurator().getConfig("config/directory.properties");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void axisAnnotator(){
        int window = rm.getInt("EVENT_DETECTOR_WINDOW");
        doc.dropAllEventsAndTimexes();
        TextAnnotation ta = doc.getTextAnnotation();
        int eiid = 0;
        List<EventTokenCandidate> eventTokenCandidateList = doc.generateAllEventTokenCandidates(window,new HashMap<>());
        for(EventTokenCandidate etc:eventTokenCandidateList){
            String label = axisLabeler.axisLabel(etc);
            if(label.toLowerCase().equals("main")){
                EventTemporalNode tmpNode = new EventTemporalNode(eiid, EventNodeType,ta.getToken(etc.getTokenId()), eiid,eiid,eiid, etc.getTokenId(),ta);
                doc.addEvent(tmpNode);
                eiid++;
            }
        }
    }

    public void tempRelAnnotator(WNSim wnsim){
        int window = rm.getInt("EVENT_TEMPREL_WINDOW");
        List<EventTemporalNode> eventList = doc.getEventList();

        // extract features
        for(EventTemporalNode e:eventList){
            e.extractPosLemmaWin(window);
            e.extractSynsets(wnsim);
        }

        for(EventTemporalNode e1:eventList){
            for(EventTemporalNode e2:eventList){
                if(e1.isEqual(e2))
                    continue;
                if(e1.getTokenId()>e2.getTokenId())
                    continue;
                TemporalRelation_EE ee = new TemporalRelation_EE(e1,e2,new TemporalRelType(TemporalRelType.relTypes.VAGUE));

                // extract features
                ee.extractSignalWords();

                TemporalRelType reltype = tempRelLabeler.tempRelLabel(ee);
                if(reltype.isNull())
                    continue;
                ee.setRelType(reltype);
                doc.getGraph().addRelNoDup(ee);
            }
        }
    }

    public void setAxisLabeler(EventAxisLabeler axisLabeler) {
        this.axisLabeler = axisLabeler;
    }

    public void setTempRelLabeler(TempRelLabeler tempRelLabeler) {
        this.tempRelLabeler = tempRelLabeler;
    }

    public static void main(String[] args) throws Exception{
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        WNSim wnsim = WNSim.getInstance(rm.getString("WordNet_Dir"));
        List<TemporalDocument> allDocs = TempEval3Reader.deserialize(rm.getString("PLATINUM_Ser"));
        HashMap<String,HashMap<Integer,String>> axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
        HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));
        String axisMdlDir = "models", axisMdlName = "eventPerceptronDetector_win2";
        EventAxisLabelerLBJ axisLabelerLBJ = new EventAxisLabelerLBJ(
                new eventDetector(axisMdlDir+ File.separator+axisMdlName+".lc",
                        axisMdlDir+File.separator+axisMdlName+".lex"));
        String temprelMdlDir = "models", temprelMldNamePrefix = "eeTempRelCls_mod0_win2";
        eeTempRelCls cls0 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+".lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+0+".lex");
        eeTempRelCls cls1 = new eeTempRelCls(temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+".lc",
                temprelMdlDir+File.separator+temprelMldNamePrefix+"_sent"+1+".lex");
        TempRelLabelerLBJ tempRelLabelerLBJ = new TempRelLabelerLBJ(cls0,cls1);

        /*String temprelMdlDir = "models", temprelMldNamePrefix = "eeTempRelCls";
        eeTempRelCls cls_mod1_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,0));
        eeTempRelCls cls_mod2_dist0 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,0),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,0));
        eeTempRelCls cls_mod1_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,1,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,1,1));
        eeTempRelCls cls_mod2_dist1 = new eeTempRelCls(String.format("%s/%s_mod%d_win2_sent%d.lc",temprelMdlDir,temprelMldNamePrefix,2,1),String.format("%s/%s_mod%d_win2_sent%d.lex",temprelMdlDir,temprelMldNamePrefix,2,1));
        TempRelLabelerLBJ tempRelLabelerLBJ = new TempRelLabelerLBJ(cls_mod1_dist0,cls_mod2_dist0,cls_mod1_dist1,cls_mod2_dist1);*/

        List<myTemporalDocument> myAllDocs = new ArrayList<>(), myAllDocs_Gold = new ArrayList<>();
        int cnt=0;
        for(TemporalDocument d:allDocs){
            String docid = d.getDocID();
            if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                continue;
            myTemporalDocument doc = new myTemporalDocument(d,0);

            myTemporalDocument docGold = new myTemporalDocument(d,1);
            docGold.keepAnchorableEvents(axisMap.get(doc.getDocid()));
            docGold.loadRelationsFromMap(relMap.get(doc.getDocid()),0);

            myAllDocs.add(doc);
            myAllDocs_Gold.add(docGold);

            TempRelAnnotator tra = new TempRelAnnotator(doc);
            tra.setAxisLabeler(axisLabelerLBJ);
            tra.axisAnnotator();
            tra.setTempRelLabeler(tempRelLabelerLBJ);
            tra.tempRelAnnotator(wnsim);
            cnt++;
            if(cnt>=20)
                break;
        }

        myTemporalDocument.NaiveEvaluator(myAllDocs_Gold,myAllDocs,1);
    }
}
