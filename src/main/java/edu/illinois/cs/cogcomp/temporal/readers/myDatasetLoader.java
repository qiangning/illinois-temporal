package edu.illinois.cs.cogcomp.temporal.readers;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.CompareCAVEO.TBDense_split;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TempRelAnnotator;
import edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TempRelLabeler;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.*;
import edu.illinois.cs.cogcomp.temporal.utils.IO.mySerialization;
import edu.illinois.cs.cogcomp.temporal.utils.myLogFormatter;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.readers.axisAnnotationReader.readAxisMapFromCrowdFlower;
import static edu.illinois.cs.cogcomp.temporal.readers.temprelAnnotationReader.readTemprelFromCrowdFlower;

public class myDatasetLoader {
    private ResourceManager rm;
    HashMap<String,HashMap<Integer,String>> axisMap;
    HashMap<String,List<temprelAnnotationReader.CrowdFlowerEntry>> relMap;

    public myDatasetLoader() {
        this("config/directory.properties");
    }

    public myDatasetLoader(String configPath) {
        try{
            rm = new temporalConfigurator().getConfig(configPath);
            axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis_oldtbdense"));
            relMap = readTemprelFromCrowdFlower(rm.getString("CF_TempRel"));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(configPath+" loading error. Exiting.");
            System.exit(-1);
        }
    }

    private List<myTemporalDocument> getDocs(String path) throws Exception{
        List<TemporalDocument> allDocs = TempEval3Reader.deserialize(path);
        List<myTemporalDocument> allDocs_new = new ArrayList<>();

        if(allDocs.size()==0){
            System.out.println("[Warning] No files were loaded from "+path);
        }
        for(TemporalDocument d:allDocs){
            myTemporalDocument doc = new myTemporalDocument(d,2);
            String docid = doc.getDocid();
            if(!axisMap.containsKey(docid)||!relMap.containsKey(docid))
                continue;
            doc.keepAnchorableEvents(axisMap.get(doc.getDocid()));
            doc.loadRelationsFromMap(relMap.get(doc.getDocid()),0);
            allDocs_new.add(doc);
        }
        return allDocs_new;
    }

    public List<myTemporalDocument> getTimeBank() throws Exception{
        return getDataset("TimeBank_Ser");
    }
    public List<myTemporalDocument> getTimeBank_Minus_TBDense() throws Exception{
        List<myTemporalDocument> files = getDataset("TimeBank_Ser");
        List<myTemporalDocument> filtered = new ArrayList<>();
        for(myTemporalDocument doc:files){
            if(TBDense_split.findDoc(doc.getDocid())==0)
                filtered.add(doc);
        }
        return filtered;
    }
    public List<myTemporalDocument> getAQUAINT() throws Exception{
        return getDataset("AQUAINT_Ser");
    }
    public List<myTemporalDocument> getPlatinum() throws Exception{
        return getDataset("PLATINUM_Ser");
    }
    public List<myTemporalDocument> getTBDense_Train() throws Exception{
        return getDataset("TBDense_Train_Ser");
    }
    public List<myTemporalDocument> getTBDense_Dev() throws Exception{
        return getDataset("TBDense_Dev_Ser");
    }
    public List<myTemporalDocument> getTBDense_Test() throws Exception{
        return getDataset("TBDense_Test_Ser");
    }
    public List<myTemporalDocument> getTCR() throws Exception{
        // rm.getString(propertyName)
        TempEval3Reader myReader = new TempEval3Reader("TIMEML", "TemporalPart", "/home/qning2/Servers/home/Research/public-TCR");
        myReader.ReadData();
        List<TemporalDocument> allDocs = myReader.getDataset().getDocuments();
        List<myTemporalDocument> allDocs_new = new ArrayList<>();

        if(allDocs.size()==0){
            System.out.println("[Warning] No files were loaded");
        }
        for(TemporalDocument d:allDocs){
            d.createTextAnnotation(myReader.getPipeline());
            myTemporalDocument doc = new myTemporalDocument(d,1);
            String docid = doc.getDocid();
            TemporalGraph graph = doc.getGraph();
            for(TLINK tlink:d.getBodyTlinks()){
                if(tlink.getSourceType().equals("timex")||tlink.getTargetType().equals("timex")) continue;
                int eiid1 = tlink.getSourceId();
                int eiid2 = tlink.getTargetId();
                EventTemporalNode sourceNode = (EventTemporalNode) graph.getNode(TemporalNode.getUniqueId("E",eiid1));
                EventTemporalNode targetNode = (EventTemporalNode) graph.getNode(TemporalNode.getUniqueId("E",eiid2));
                TemporalRelType rel;
                switch (tlink.getReducedRelType().toStringfull()) {
                    case "before":
                        rel = new TemporalRelType(TemporalRelType.relTypes.BEFORE);
                        break;
                    case "after":
                        rel = new TemporalRelType(TemporalRelType.relTypes.AFTER);
                        break;
                    case "equal":
                        rel = new TemporalRelType(TemporalRelType.relTypes.EQUAL);
                        break;
                    default:
                        rel = new TemporalRelType(TemporalRelType.relTypes.VAGUE);
                }
                TemporalRelation_EE tmpRel = new TemporalRelation_EE(sourceNode, targetNode, rel, doc);
                graph.addRelNoDup(tmpRel);
            }
            allDocs_new.add(doc);
        }
        return allDocs_new;
    }
    public List<myTemporalDocument> getDataset(String propertyName) throws Exception{
        return getDocs(rm.getString(propertyName));
    }

    public List<myTemporalDocument> getTimeBank_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TimeBank_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getTimeBank_Minus_TBDense_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TimeBank_Minus_TBDense_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getAQUAINT_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("AQUAINT_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getPlatinum_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("PLATINUM_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getTBDense_Train_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TBDense_Train_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getTBDense_Dev_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TBDense_Dev_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getTBDense_Test_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TBDense_Test_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getTCR_autoCorrected() throws Exception{
        return getDatasetAutoCorrected("TCR_Ser_AutoCorrected");
    }
    public List<myTemporalDocument> getDatasetAutoCorrected(String propertyName) throws Exception{
        if(propertyName.equals("TimeBank_Minus_TBDense_Ser_AutoCorrected")){
            List<myTemporalDocument> files = getDatasetAutoCorrected("TimeBank_Ser_AutoCorrected");
            List<myTemporalDocument> filtered = new ArrayList<>();
            for(myTemporalDocument doc:files){
                if(TBDense_split.findDoc(doc.getDocid())==0)
                    filtered.add(doc);
            }
            return filtered;
        }
        String dir = rm.getString(propertyName);
        System.out.println(myLogFormatter.startBlockLog("Loading "+dir));
        mySerialization myser = new mySerialization(false);
        List<myTemporalDocument> allDocs = new ArrayList<>();
        File file = new File(dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            System.out.println(dir+" is empty.");
            return null;
        }
        System.out.println("Loading from "+dir+". In total "+filelist.length+" files.");
        for(File f:filelist){
            if(f.isFile()) {
                if (f.getName().equals(".DS_Store"))
                    continue;
                //deserialize doc
                myTemporalDocument doc = (myTemporalDocument) myser.deserialize(f.getPath());
                /*serialization doesn't exist.*/
                if(doc==null){
                    continue;
                }
                allDocs.add(doc);
            }
        }
        System.out.println(myLogFormatter.endBlockLog("Loading "+dir));
        return allDocs;
    }

    public List<myTemporalDocument> getDatasetAutoCorrected(String[] propertyNames) throws Exception{
        List<myTemporalDocument> allDocs = new ArrayList<>();
        for(String str:propertyNames){
            System.out.printf("Loading %s...\n",str);
            allDocs.addAll(getDatasetAutoCorrected(str));
        }
        return allDocs;
    }

    public void extractAllFeats(List<myTemporalDocument> docs,int win){
        for(myTemporalDocument d:docs)
            d.extractAllFeats(win);
    }

    private class TempRelLabeler_Gold extends TempRelLabeler{
        @Override
        public TemporalRelType tempRelLabel(TemporalRelation ee) {
            return ee.getRelType();
        }

        @Override
        public boolean isIgnore(TemporalRelation ee) {
            return ! (ee instanceof TemporalRelation_EE)
                    ||ee.isNull();
        }
    }
    public void autoTempRelCorrectionViaILP(List<myTemporalDocument> docs){
        for(myTemporalDocument doc:docs) {
            TempRelAnnotator tra = new TempRelAnnotator(doc, null, new TempRelLabeler_Gold(),null,rm);
            TempRelAnnotator.performET = false;
            TempRelAnnotator.ilp = true;
            tra.setup(true, true, true, false);
            tra.annotator();
        }
    }

    public static void main(String[] args) throws Exception {
        myDatasetLoader loader = new myDatasetLoader();
        mySerialization myser = new mySerialization(true);
        List<myTemporalDocument> docs;

        docs = loader.getTCR();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/TCR/"+doc.getDocid()+".ser");
        }

        /*docs = loader.getTBDense_Test();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/TBDense_Test/"+doc.getDocid()+".ser");
        }
        docs = loader.getTBDense_Dev();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/TBDense_Dev/"+doc.getDocid()+".ser");
        }
        docs = loader.getTBDense_Train();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/TBDense_Train/"+doc.getDocid()+".ser");
        }

        docs = loader.getTimeBank();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/TimeBank/"+doc.getDocid()+".ser");
        }

        docs = loader.getAQUAINT();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/AQUAINT/"+doc.getDocid()+".ser");
        }

        docs = loader.getPlatinum();
        loader.autoTempRelCorrectionViaILP(docs);
        for(myTemporalDocument doc:docs) {
            myser.serialize(doc,"serialization/myTemporalDocument/te3-platinum/"+doc.getDocid()+".ser");
        }*/
    }
}
