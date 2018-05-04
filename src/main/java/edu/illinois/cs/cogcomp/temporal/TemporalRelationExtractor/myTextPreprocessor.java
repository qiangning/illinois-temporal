package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.myTextAnnotationUtilities;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.temporal.datastruct.Temporal.myTemporalDocument;

public class myTextPreprocessor {
    private AnnotatorService annotator;

    public myTextPreprocessor() throws Exception{
        annotator = CuratorFactory.buildCuratorClient();
    }

    public TextAnnotation extractTextAnnotation(String text)  throws Exception{
        TextAnnotation ta = annotator.createBasicTextAnnotation("","", text);
        try {
            //annotator.addView(ta, ViewNames.LEMMA);//lemma here leads to a bug because text has a leading "\n", which messed up the token offset of lemma annotator.
            annotator.addView(ta, ViewNames.SENTENCE);
        }
        catch (Exception e){
            System.out.printf("Adding SENTENCE view failed for text %s\n",text);
            e.printStackTrace();
        }
        TextAnnotation[] sentTa = new TextAnnotation[ta.sentences().size()];
        for(int sentenceId=0;sentenceId<ta.sentences().size();sentenceId++){// without this, empty views of ta will be added
            sentTa[sentenceId]= TextAnnotationUtilities.getSubTextAnnotation(ta, sentenceId);
        }
        for (int sentenceId = 0; sentenceId < ta.sentences().size(); ++sentenceId) {
            try {
                annotator.addView(sentTa[sentenceId], ViewNames.LEMMA);
                annotator.addView(sentTa[sentenceId], ViewNames.POS);
                annotator.addView(sentTa[sentenceId], ViewNames.SRL_VERB);
                /*annotator.addView(sentTa[sentenceId], ViewNames.SRL_NOM);
                annotator.addView(sentTa[sentenceId], ViewNames.SRL_PREP);*/
            } catch (Exception e) {
                System.out.printf("Adding SRL view failed for doc %s sentId=%d\n","",sentenceId);
                e.printStackTrace();
            }
            int start = ta.getSentence(sentenceId).getStartSpan();
            int end = ta.getSentence(sentenceId).getEndSpan();
            myTextAnnotationUtilities.copyViewsFromTo(sentTa[sentenceId],ta, start, end, start);
        }
        return ta;
    }

    public static void main(String[] args) throws Exception{
        String text = "I fell in love with her after I first met her 9 years ago. Now we are expecting our first baby this June.";
        myTemporalDocument doc = new myTemporalDocument(text,"test","2010-05-04");
        TempRelAnnotator tra = new TempRelAnnotator(doc);
        tra.annotator();
        System.out.println();
    }
}
