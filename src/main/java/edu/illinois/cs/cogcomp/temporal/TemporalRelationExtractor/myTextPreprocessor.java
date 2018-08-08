package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

import java.util.List;

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
                annotator.addView(sentTa[sentenceId], ViewNames.DEPENDENCY);
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
        String text = "I failed to do it.";
        TextAnnotation ta = exc.extractTextAnnotation(text);
        TreeView dep = (TreeView) ta.getView(ViewNames.DEPENDENCY);
        String[] toks = ta.getTokens();
        for(int i=0;i<toks.length;i++){
            List<Constituent> clist = dep.getConstituentsCoveringToken(i);
            for(Constituent c:clist){
                List<Relation> rels = c.getOutgoingRelations();
                for(Relation r:rels){
                    System.out.println(r);
                    System.out.println(r.getRelationName());
                }
                rels = c.getIncomingRelations();
                for(Relation r:rels){
                    int tokid = r.getTarget().getStartSpan();
                    System.out.println(r.getSource());
                    System.out.println(r.getTarget());
                }
                System.out.println();
            }
        }
        System.out.println();
    }
}
