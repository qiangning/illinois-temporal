package edu.illinois.cs.cogcomp.temporal.readers;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.temporal.utils.myIOUtils;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.HashMap;
import java.util.List;

public class axisAnnotationReader {
    public static String LABEL_NOT_ON_ANY_AXIS = "null";
    public static String axis_label_conversion(String label){
        switch (label){
            case "yes_its_anchorable":
                return "main";
            case "no_its_intentionwishopinion":
                //return "orthogonal";
            case "no_its_negation":
                //return "negation";
            case "no_its_hypotheticalcondition":
            case "no_its_recurrent":
            case "no_its_static":
            case "no_its_abstractnonspecific":
                return "others";
            default:
                return LABEL_NOT_ON_ANY_AXIS;
        }
    }
    public static HashMap<Integer,Integer> eventTokId2Index(TemporalDocument doc){
        // tokenId-->eventid
        HashMap<Integer,Integer> ret = new HashMap<>();
        TextAnnotation ta = doc.getTextAnnotation();
        List<EventChunk> eventList = doc.getBodyEventMentions();
        for(EventChunk ec:eventList){
            int tokenId = ta.getTokenIdFromCharacterOffset(ec.getCharStart());
            ret.put(tokenId,eventList.indexOf(ec));
        }
        return ret;
    }
    public static HashMap<Integer,Integer> eventIndex2TokId(TemporalDocument doc){
        // eventid-->tokenId
        HashMap<Integer,Integer> ret = new HashMap<>();
        TextAnnotation ta = doc.getTextAnnotation();
        List<EventChunk> eventList = doc.getBodyEventMentions();
        for(EventChunk ec:eventList){
            int tokenId = ta.getTokenIdFromCharacterOffset(ec.getCharStart());
            ret.put(eventList.indexOf(ec),tokenId);
        }
        return ret;
    }
    public static HashMap<String,HashMap<Integer,String>> readAxisMapFromCrowdFlower(String fileList){
        // docid-->eventid-->axis_label
        HashMap<String,HashMap<Integer,String>> axisMap = new HashMap<>();
        String[] files = fileList.split(",");
        for(String file:files){
            String tmpDir = myIOUtils.getParentDir(file);
            String tmpFile = myIOUtils.getFileOrDirName(file);
            myCSVReader cf_reader = new myCSVReader(tmpDir,tmpFile);
            for(int i=0;i<cf_reader.getContentLines();i++){
                try {
                    String docid = cf_reader.getLineTag(i, "docid");
                    int eventid = Integer.valueOf(cf_reader.getLineTag(i, "eventid"));
                    String anchorability = cf_reader.getLineTag(i, "can_the_verb_span_stylecolorblueverb_span_be_anchored_in_time");
                    if(!axisMap.containsKey(docid))
                        axisMap.put(docid,new HashMap<>());
                    axisMap.get(docid).put(eventid,anchorability);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return axisMap;
    }
}
