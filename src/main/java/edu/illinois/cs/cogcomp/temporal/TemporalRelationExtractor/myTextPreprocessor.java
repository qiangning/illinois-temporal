package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.annotation.BasicAnnotatorService;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.chunker.main.ChunkerAnnotator;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.depparse.DepAnnotator;
import edu.illinois.cs.cogcomp.nlp.lemmatizer.IllinoisLemmatizer;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import edu.illinois.cs.cogcomp.srl.SemanticRoleLabeler;
import edu.illinois.cs.cogcomp.srl.config.SrlConfigurator;
import edu.illinois.cs.cogcomp.srl.core.SRLType;

import java.util.*;

public class myTextPreprocessor {
    private BasicAnnotatorService annotator;

    public myTextPreprocessor() throws Exception{
        annotator = PipelineFactory.buildPipeline(
                ViewNames.POS,
                ViewNames.SRL_VERB,
                ViewNames.DEPENDENCY,
                ViewNames.LEMMA
        );
    }

    public TextAnnotation extractTextAnnotation(String text)  throws Exception{
        TextAnnotation ta = annotator.createAnnotatedTextAnnotation("", "", text);
        TextAnnotation[] sentTa = new TextAnnotation[ta.sentences().size()];
        for(int sentenceId=0;sentenceId<ta.sentences().size();sentenceId++){// without this, empty views of ta will be added
            sentTa[sentenceId]= TextAnnotationUtilities.getSubTextAnnotation(ta, sentenceId);
        }
        for (int sentenceId = 0; sentenceId < ta.sentences().size(); ++sentenceId) {
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
