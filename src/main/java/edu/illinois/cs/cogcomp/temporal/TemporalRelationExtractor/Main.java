package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.temporal.configurations.temporalConfigurator;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.EventTemporalNode;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelType;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.TemporalRelation_EE;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;
import edu.illinois.cs.cogcomp.temporal.utils.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.temporal.utils.IO.myIOUtils;
import edu.illinois.cs.cogcomp.temporal.utils.IO.mySerialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrieveLamma_Span;
import static edu.illinois.cs.cogcomp.temporal.utils.myUtils4TextAnnotation.retrievePOS_Span;

public class Main {
    public static ResourceManager rm;
    public static String sersuffix = ".ser";
    public static String timelinesuffix = ".timeline";
    public static String temprelsuffix = ".temprel";
    public static String statssuffix = ".stats";
    public static String splitter = "///";
    public static boolean forceUpdate = false;
    public static int MAX_NUM_EVENT = 100;
    public static void postProcessing(String input_doc_ser_dir, String output_dir, String log_dir, String log_name) throws Exception{
        myIOUtils.mkdir(log_dir);
        myIOUtils.mkdir(output_dir);
        PrintStream ps = new PrintStream(new FileOutputStream(log_dir + File.separator + log_name, true));
        ps.println("-----START-----");
        ps.println("Message: Retrieve TempRels from the serialized myTemporalDocument objects");
        ps.println(ZonedDateTime.now());
        try{
            InetAddress addr = InetAddress.getLocalHost();
            ps.println("Host address: "+addr.getHostAddress());
            ps.println("Host: "+addr.getHostName());
        }
        catch (Exception e){
            ps.println("Host: Unavailable.");
        }
        mySerialization ser = new mySerialization(false);
        File file = new File(input_doc_ser_dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            ps.println(input_doc_ser_dir+" is empty.");
            return;
        }
        ps.println("Loading from "+input_doc_ser_dir+". In total "+filelist.length+" files.");
        int cnt_process=0, cnt_skip=0, cnt_fail=0;
        HashMap<String,int[][]> stats = new HashMap<>();// key: "v1///v2", val[i][j]: # when sent diff=i, rel=j
        File output_stats = new File(output_dir+File.separator+file.getName()+statssuffix);
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        for(File f:filelist){
            if(!f.isFile()) continue;
            if (!f.getName().endsWith("xml.ser"))
                continue;
            File output_temprel = new File(output_dir+File.separator+f.getName()+ temprelsuffix);
            if(!forceUpdate&&output_temprel.exists()){
                ps.printf("%s exists. Skipped.\n", f.getName());
                cnt_skip++;
                continue;
            }
            ps.printf("%s processing...",f.getName());
            ExecutionTimeUtil timer2 = new ExecutionTimeUtil();
            timer2.start();
            try {
                myTemporalDocument doc = (myTemporalDocument) ser.deserialize(f.getAbsolutePath());
                doc.getGraph().reduction();
                cnt_process++;
                PrintStream ps_temprel = new PrintStream(new FileOutputStream(output_temprel));
                for(TemporalRelation_EE ee:doc.getGraph().getAllEERelations(-1)){
                    if(ee.getSentDiff()>2) continue;
                    // lemma1,lemma2,relation,e1 sent id,e2 sent id
                    EventTemporalNode event1 = ee.getSourceNode();
                    EventTemporalNode event2 = ee.getTargetNode();
                    ps_temprel.printf("%s:%s///%s:%s///%s///%d///%d\n",event1.getUniqueId(),event1.getLemma(),event2.getUniqueId(),event2.getLemma(),ee.getRelType().getReltype().getName(),event1.getSentId(),event2.getSentId());
                    String key = event1.getLemma()+splitter+event2.getLemma();
                    if(!stats.containsKey(key))
                        stats.put(key,new int[3][5]);
                    stats.get(key)[ee.getSentDiff()][ee.getRelType().getReltype().getIndex()]++;
                }
                ps_temprel.close();
            }
            catch (Exception e){
                ps.printf("\t%s failed: %s",f.getName(),e.toString());
                cnt_fail++;
            }
            timer2.end();
            ps.printf("\t(%d ms)\n", timer2.getTimeMillis());
        }
        // print out stats
        PrintStream ps_stats = new PrintStream(new FileOutputStream(output_stats),true);
        List<String> allKeys = new ArrayList<>(stats.keySet());
        allKeys.sort((k1,k2)->k1.compareTo(k2));
        for(String key:allKeys){
            String[] tmp = key.split(splitter);
            if(tmp.length!=2) continue;
            String v1 = tmp[0];
            String v2 = tmp[1];
            int[][] counts = stats.get(key);
            for(int i=0;i< TemporalRelType.relTypes.values().length;i++){
                if(counts[0][i]+counts[1][i]+counts[2][i]==0)
                    continue;
                ps_stats.printf("%s,%s,%s,%d,%d,%d\n",v1,v2,TemporalRelType.relTypes.getAllNames()[i],counts[0][i],counts[1][i],counts[2][i]);
            }
        }
        ps_stats.close();

        timer.end();
        ps.printf("Total #Files processed: %d\nTotal #Files skipped: %d\nTotal #Files failed: %d\nTotal time elapsed: %.2f minutes\n",cnt_process,cnt_skip,cnt_fail,1.0*timer.getTimeMillis()/1000/60);
        ps.printf("Average processing time: %.2f seconds/doc\n",cnt_process>0?1.0*timer.getTimeMillis()/1000/cnt_process:0);
        ps.println(ZonedDateTime.now());
        ps.println("-----FINISH-----\n");
        ps.close();
    }
    public static void TempRelExtractor(String input_ta_dir, String output_dir, String log_dir, String log_name) throws Exception{
        rm = new temporalConfigurator().getConfig("config/directory.properties","config/TempRelAnnotator.aws.properties");
        myIOUtils.mkdir(log_dir);
        PrintStream ps = new PrintStream(new FileOutputStream(log_dir + File.separator + log_name, true));
        ps.println("-----START-----");
        ps.println("forceUpdate="+forceUpdate);
        ps.println("MAX_NUM_EVENT="+MAX_NUM_EVENT);
        ps.println(ZonedDateTime.now());
        try{
            InetAddress addr = InetAddress.getLocalHost();
            ps.println("Host address: "+addr.getHostAddress());
            ps.println("Host: "+addr.getHostName());
        }
        catch (Exception e){
            ps.println("Host: Unavailable.");
        }
        mySerialization ser = new mySerialization(false);
        File file = new File(input_ta_dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            ps.println(input_ta_dir+" is empty.");
            return;
        }
        myIOUtils.mkdir(output_dir);
        ps.println("Loading from "+input_ta_dir+". In total "+filelist.length+" files.");
        int cnt_process=0, cnt_skip=0, cnt_fail=0;
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        for(File f:filelist){
            if(!f.isFile()) continue;
            if (!f.getName().endsWith("xml"))
                continue;
            File output_ser = new File(output_dir+File.separator+f.getName()+ sersuffix);
            File output_timeline = new File(output_dir+File.separator+f.getName()+ timelinesuffix);
            if(!forceUpdate&&output_ser.exists()&&output_timeline.exists()){
                ps.printf("%s exists. Skipped.\n", f.getName());
                cnt_skip++;
                continue;
            }
            ps.printf("%s processing...",f.getName());
            ExecutionTimeUtil timer2 = new ExecutionTimeUtil();
            timer2.start();
            try {
                myTemporalDocument doc;
                boolean flag = true;
                if(!forceUpdate&&output_ser.exists()){
                    doc = (myTemporalDocument) ser.deserialize(output_ser.getAbsolutePath());
                }
                else{
                    TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath(), true);
                    doc = new myTemporalDocument(ta, f.getName());
                    TempRelAnnotator tra = new TempRelAnnotator(doc, rm);
                    flag = tra.annotator(MAX_NUM_EVENT);
                }
                ps.printf("\t#E=%d,#T=%d", doc.getEventList().size(), doc.getTimexList().size());
                if(flag) {
                    doc.getGraph().reduction();
                    ser.serialize(doc, output_ser.getAbsolutePath());
                    cnt_process++;
                    PrintStream ps_timeline = new PrintStream(new FileOutputStream(output_timeline));
                    List<EventTemporalNode> timeline = doc.getGraph().convert2chain();
                    for (EventTemporalNode etn : timeline) {
                        IntPair charSpan = etn.getTa().getTokenCharacterOffset(etn.getTokenId());
                        ps_timeline.printf("%s/%d/[%d %d]/%s/%s/%s\n", etn.getUniqueId(), etn.getTokenId(), charSpan.getFirst(), charSpan.getSecond(), etn.getText(), etn.getLemma(), etn.getPos());
                        if (etn.getVerb_srl() != null) {
                            StringBuilder sb = new StringBuilder();
                            List<Relation> outgoingRelations = new ArrayList<>(etn.getVerb_srl().getOutgoingRelations());
                            outgoingRelations.sort(Comparator.comparing(Relation::getRelationName));

                            for (Relation r : outgoingRelations) {
                                Constituent target = r.getTarget();
                                if (r.getRelationName().charAt(1) > '9' || r.getRelationName().charAt(1) < '0')
                                    continue;
                                sb.append(String.format("[%s] [%d %d] [%d %d] %s || %s || %s\n", r.getRelationName(), target.getStartSpan(), target.getEndSpan(), target.getStartCharOffset(), target.getEndCharOffset(), target.getTokenizedSurfaceForm(), retrieveLamma_Span(doc.getTextAnnotation(), target.getSpan()), retrievePOS_Span(doc.getTextAnnotation(), target.getSpan())));
                            }
                            ps_timeline.print(sb);
                        }
                        ps_timeline.println();
                    }
                }
                else{
                    ps.print("\tToo many events. Skipped.");
                    cnt_skip++;
                }
            }
            catch (Exception e){
                ps.printf("\t%s failed: %s",f.getName(),e.toString());
                cnt_fail++;
            }
            timer2.end();
            ps.printf("\t(%d ms)\n", timer2.getTimeMillis());
        }
        timer.end();
        ps.printf("Total #Files processed: %d\nTotal #Files skipped: %d\nTotal #Files failed: %d\nTotal time elapsed: %.2f minutes\n",cnt_process,cnt_skip,cnt_fail,1.0*timer.getTimeMillis()/1000/60);
        ps.printf("Average processing time: %.2f seconds/doc\n",cnt_process>0?1.0*timer.getTimeMillis()/1000/cnt_process:0);
        ps.println(ZonedDateTime.now());
        ps.println("-----FINISH-----\n");
        ps.close();
    }
    public static void main(String[] args) throws Exception {
        String input_ta_dir = args[0];
        String output_dir = args[1];
        String log_dir = args[2];
        String log_name = args[3];
        /*Main.forceUpdate = Boolean.valueOf(args[4]);
        Main.MAX_NUM_EVENT = Integer.valueOf(args[5]);
        TempRelExtractor(input_ta_dir,output_dir,log_dir,log_name);*/
        postProcessing(input_ta_dir,output_dir,log_dir,log_name);
    }
}
