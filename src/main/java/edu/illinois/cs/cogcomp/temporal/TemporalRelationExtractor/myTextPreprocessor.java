package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.myTextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

public class myTextPreprocessor {
    private AnnotatorService annotator;

    public myTextPreprocessor() throws Exception{
        annotator = CuratorFactory.buildCuratorClient();
    }

    public TextAnnotation extractTextAnnotationPipeline(String text) throws Exception{
        ResourceManager userConfig = new ResourceManager("config/pipeline-config.properties");
        AnnotatorService pipeline = PipelineFactory.buildPipeline(userConfig);
        TextAnnotation ta = pipeline.createAnnotatedTextAnnotation( "", "", text );
        return ta;
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
        myTextPreprocessor exc = new myTextPreprocessor();
        String text = "Helicopters patrol the temporary no-fly zone around New Jersey's MetLife Stadium Sunday, with F-16s based in Atlantic City ready to be scrambled if an unauthorized aircraft does enter the restricted airspace.";
        TextAnnotation ta = exc.extractTextAnnotation(text);
        System.out.println();
    }
}
