package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

import java.util.List;

public class myTextPreprocessor {
    private AnnotatorService annotator;
    private TokenizerTextAnnotationBuilder tab;
    public static boolean useCuratorOrPipeline = true; // true->curator. false->pipeline

    public myTextPreprocessor() throws Exception{
        if(useCuratorOrPipeline)
            annotator = CuratorFactory.buildCuratorClient();
        else
            annotator = PipelineFactory.buildPipeline(
                    ViewNames.POS,
                    ViewNames.SHALLOW_PARSE,
                    ViewNames.PARSE_STANFORD,
                    ViewNames.NER_CONLL,
                    ViewNames.LEMMA,
                    ViewNames.SRL_VERB
            );
        tab = new TokenizerTextAnnotationBuilder(new StatefulTokenizer());
    }

    public TextAnnotation extractTextAnnotation(String text)  throws Exception{
        TextAnnotation ta;
        if(useCuratorOrPipeline){
            ta = annotator.createBasicTextAnnotation("","", text);
            try {
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
                    annotator.addView(sentTa[sentenceId], ViewNames.DEPENDENCY);
                    annotator.addView(sentTa[sentenceId], ViewNames.DEPENDENCY_STANFORD);
                    annotator.addView(sentTa[sentenceId], ViewNames.SRL_VERB);
                    annotator.addView(sentTa[sentenceId], ViewNames.SRL_NOM);
                    annotator.addView(sentTa[sentenceId], ViewNames.SRL_PREP);
                    annotator.addView(sentTa[sentenceId], ViewNames.NER_CONLL);
                } catch (Exception e) {
                    System.out.printf("Adding SRL view failed for doc %s sentId=%d\n","",sentenceId);
                    e.printStackTrace();
                }
                int start = ta.getSentence(sentenceId).getStartSpan();
                int end = ta.getSentence(sentenceId).getEndSpan();
                myTextAnnotationUtilities.copyViewsFromTo(sentTa[sentenceId],ta, start, end, start);
            }
        }
        else {
            ta = tab.createTextAnnotation(text);
            TextAnnotation[] sentTa = new TextAnnotation[ta.sentences().size()];
            for (int sentenceId = 0; sentenceId < ta.sentences().size(); sentenceId++) {// without this, empty views of ta will be added
                sentTa[sentenceId] = TextAnnotationUtilities.getSubTextAnnotation(ta, sentenceId);
                annotator.annotateTextAnnotation(sentTa[sentenceId], true);
            }
            for (int sentenceId = 0; sentenceId < ta.sentences().size(); ++sentenceId) {
                int start = ta.getSentence(sentenceId).getStartSpan();
                int end = ta.getSentence(sentenceId).getEndSpan();
                myTextAnnotationUtilities.copyViewsFromTo(sentTa[sentenceId], ta, start, end, start);
            }
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