package edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.myTextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

public class myTextPreprocessor {
    private AnnotatorService annotator;
    private TokenizerTextAnnotationBuilder tab;
    public boolean useCuratorOrPipeline = true; // true->curator. false->pipeline
    public static myTextPreprocessor instance;
    private ResourceManager rm;

    public myTextPreprocessor() throws Exception{
        this(new ResourceManager("config/preprocessor.properties"));
    }
    public myTextPreprocessor(ResourceManager rm) throws Exception{
        this.rm = rm;
        useCuratorOrPipeline = rm.getBoolean("useCurator");
        if(useCuratorOrPipeline){
            annotator = CuratorFactory.buildCuratorClient();
        }
        else{
            annotator = PipelineFactory.buildPipeline(rm);
        }
        tab = new TokenizerTextAnnotationBuilder(new StatefulTokenizer());
    }
    public TextAnnotation extractTextAnnotation(TextAnnotation ta, String text)  throws Exception{
        if(useCuratorOrPipeline){
            if(ta==null)
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
                    if(rm.getBoolean("useLemma")&&!sentTa[sentenceId].hasView(ViewNames.LEMMA)) annotator.addView(sentTa[sentenceId], ViewNames.LEMMA);
                    if(rm.getBoolean("usePos")&&!sentTa[sentenceId].hasView(ViewNames.POS)) annotator.addView(sentTa[sentenceId], ViewNames.POS);
                    if(rm.getBoolean("useShallowParse")&&!sentTa[sentenceId].hasView(ViewNames.SHALLOW_PARSE)) annotator.addView(sentTa[sentenceId], ViewNames.SHALLOW_PARSE);
                    if(rm.getBoolean("useSrlVerb")&&!sentTa[sentenceId].hasView(ViewNames.SRL_VERB)) annotator.addView(sentTa[sentenceId], ViewNames.SRL_VERB);
                    if(rm.getBoolean("useDep")&&!sentTa[sentenceId].hasView(ViewNames.DEPENDENCY)) annotator.addView(sentTa[sentenceId], ViewNames.DEPENDENCY);
                    if(rm.getBoolean("useStanfordDep")&&!sentTa[sentenceId].hasView(ViewNames.DEPENDENCY_STANFORD)) annotator.addView(sentTa[sentenceId], ViewNames.DEPENDENCY_STANFORD);
                    if(rm.getBoolean("useNerConll")&&!sentTa[sentenceId].hasView(ViewNames.NER_CONLL)) annotator.addView(sentTa[sentenceId], ViewNames.NER_CONLL);
                    if(rm.getBoolean("useSrlNom")&&!sentTa[sentenceId].hasView(ViewNames.SRL_NOM)) annotator.addView(sentTa[sentenceId], ViewNames.SRL_NOM);
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
            if(ta==null)
                ta = tab.createTextAnnotation(text);
            TextAnnotation[] sentTa = new TextAnnotation[ta.sentences().size()];
            for (int sentenceId = 0; sentenceId < ta.sentences().size(); sentenceId++) {// without this, empty views of ta will be added
                sentTa[sentenceId] = TextAnnotationUtilities.getSubTextAnnotation(ta, sentenceId);
                annotator.annotateTextAnnotation(sentTa[sentenceId], false);
            }
            for (int sentenceId = 0; sentenceId < ta.sentences().size(); ++sentenceId) {
                int start = ta.getSentence(sentenceId).getStartSpan();
                int end = ta.getSentence(sentenceId).getEndSpan();
                myTextAnnotationUtilities.copyViewsFromTo(sentTa[sentenceId], ta, start, end, start);
            }
        }
        return ta;
    }
    public static myTextPreprocessor getInstance() throws Exception{
        if(instance==null) {
            instance = new myTextPreprocessor();
            return instance;
        }
        return instance;
    }
    public static void main(String[] args) throws Exception{
        myTextPreprocessor exc = myTextPreprocessor.getInstance();
        String text = "I failed to do it.";
        TextAnnotation ta = exc.extractTextAnnotation(null,text);
        System.out.println(ta.getAvailableViews());
        /*TreeView dep = (TreeView) ta.getView(ViewNames.DEPENDENCY);
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
        System.out.println();*/
    }
}