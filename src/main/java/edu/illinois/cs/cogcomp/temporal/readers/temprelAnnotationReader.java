package edu.illinois.cs.cogcomp.temporal.readers;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.utils.myIOUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class temprelAnnotationReader {
    public static class Q1_Q2_temprel{
        private boolean q1, q2;
        private TemporalRelType relType;
        public Q1_Q2_temprel(boolean q1, boolean q2){
            this.q1 = q1;
            this.q2 = q2;
            if(q1&&q2)
                relType = new TemporalRelType(TemporalRelType.relTypes.VAGUE);
            else if(q1&&!q2)
                relType = new TemporalRelType(TemporalRelType.relTypes.BEFORE);
            else if(!q1&&q2)
                relType = new TemporalRelType(TemporalRelType.relTypes.AFTER);
            else
                relType = new TemporalRelType(TemporalRelType.relTypes.EQUAL);
        }

        public boolean isQ1() {
            return q1;
        }

        public boolean isQ2() {
            return q2;
        }

        public TemporalRelType getRelType() {
            return relType;
        }

        @Override
        public String toString() {
            return relType.toString();
        }

        public Q1_Q2_temprel(TemporalRelType relType) {
            this.relType = relType;
            switch (relType.toString().toLowerCase()){
                case "vague":
                    q1 = true;
                    q2 = true;
                    break;
                case "before":
                    q1 = true;
                    q2 = false;
                    break;
                case "after":
                    q1 = false;
                    q2 = true;
                    break;
                case "equal":
                    q1 = false;
                    q2 = false;
                    break;
                default:
                    System.out.println("Unexpected relType in Q1_Q2_temprel");
                    System.exit(-1);
            }

        }
    }
    public static class CrowdFlowerEntry{
        private int eventid1, eventid2;// eiid
        private Q1_Q2_temprel rel;

        public int getEventid1() {
            return eventid1;
        }

        public int getEventid2() {
            return eventid2;
        }

        public Q1_Q2_temprel getRel() {
            return rel;
        }

        public CrowdFlowerEntry(int eventid1, int eventid2, Q1_Q2_temprel rel) {
            this.eventid1 = eventid1;
            this.eventid2 = eventid2;
            this.rel = rel;
        }

        @Override
        public String toString() {
            return "CrowdFlowerEntry{" +
                    "eventid1=" + eventid1 +
                    ", eventid2=" + eventid2 +
                    ", rel=" + rel +
                    '}';
        }
    }
    public static HashMap<String,List<CrowdFlowerEntry>> readTemprelFromCrowdFlower(String fileList){
        // docid-->CrowdFlowerEntry
        HashMap<String,List<CrowdFlowerEntry>> relMap = new HashMap<>();
        String[] files = fileList.split(",");
        for(String file:files){
            String tmpDir = myIOUtils.getParentDir(file);
            String tmpFile = myIOUtils.getFileOrDirName(file);
            myCSVReader cf_reader = new myCSVReader(tmpDir,tmpFile);
            for(int i=0;i<cf_reader.getContentLines();i++){
                try {
                    String docid = cf_reader.getLineTag(i, "docid");
                    int eventid1 = Integer.valueOf(cf_reader.getLineTag(i, "eventid1"));
                    int eventid2 = Integer.valueOf(cf_reader.getLineTag(i, "eventid2"));
                    boolean q1 = cf_reader.getLineTag(i,"q1").equals("yes");
                    boolean q2 = cf_reader.getLineTag(i,"q2").equals("yes");
                    Q1_Q2_temprel temprel = new Q1_Q2_temprel(q1,q2);
                    CrowdFlowerEntry entry = new CrowdFlowerEntry(eventid1,eventid2,temprel);
                    if(!relMap.containsKey(docid))
                        relMap.put(docid,new ArrayList<>());
                    relMap.get(docid).add(entry);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return relMap;
    }
    public static void main(String[] args) throws Exception{
        ResourceManager rm = new temporalConfigurator().getConfig("config/directory.properties");
        HashMap<String,List<CrowdFlowerEntry>> relMap = readTemprelFromCrowdFlower(rm.getString("CF_TimeBank_TempRel"));
        System.out.println();
    }
}
