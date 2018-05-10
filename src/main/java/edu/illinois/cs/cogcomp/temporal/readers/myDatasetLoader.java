package edu.illinois.cs.cogcomp.temporal.readers;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.CompareCAVEO.TBDense_split;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

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
            axisMap = readAxisMapFromCrowdFlower(rm.getString("CF_Axis"));
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
            myTemporalDocument doc = new myTemporalDocument(d,1);
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
    public List<myTemporalDocument> getDataset(String propertyName) throws Exception{
        return getDocs(rm.getString(propertyName));
    }

    public void extractAllFeats(List<myTemporalDocument> docs,int win){
        for(myTemporalDocument d:docs)
            d.extractAllFeats(win);
    }
}
