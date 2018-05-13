package edu.illinois.cs.cogcomp.temporal.utils;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.lbjava.learn.Learner;
import edu.illinois.cs.cogcomp.temporal.explorations.MultiClassifiers;
import edu.illinois.cs.cogcomp.temporal.utils.mySerialization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CoDLWrapper_LBJ<LearningStruct, LearningAtom> {
    protected List<LearningStruct> trainStructs_partial;
    protected int maxRound, seed;
    protected boolean OneMdlOrTwoMdl;// true: 1-model. false: 2-model
    protected MultiClassifiers<LearningAtom> multiClassifiers;// this could also be single classifier (i.e., length=1)
    protected String modelDir, modelNamePrefix;
    protected String cacheDir;
    protected boolean forceUpdate = false;
    protected mySerialization serializer = new mySerialization(true);

    // needed only if using 1-model
    protected List<LearningStruct> trainStructs_full;

    // needed only if using 2-model
    protected double lambda;// must be in [0,1]

    public CoDLWrapper_LBJ(boolean OneMdlOrTwoMdl,int maxRound, int seed, String modelDir, String modelNamePrefix) throws Exception{
        this.OneMdlOrTwoMdl = OneMdlOrTwoMdl;
        this.maxRound = maxRound;
        this.seed = seed;
        setModelPath(modelDir,modelNamePrefix);
        Learner cls0 = loadBaseCls();
        if(OneMdlOrTwoMdl) {
            loadData_1model();
            multiClassifiers = new MultiClassifiers<>(cls0,-1,true);
        }
        else {
            loadData_2model();
            multiClassifiers = new MultiClassifiers<>(cls0,lambda,true);
            multiClassifiers.addClassifier(cls0);// this is just a placeholder
        }
        setCacheDir();
    }

    // 1-model
    /*public CoDLWrapper_LBJ(Learner cls0, List<LearningStruct> trainStructs_partial, int maxRound, int seed, List<LearningStruct> trainStructs_full,String modelDir, String modelNamePrefix) {
        OneMdlOrTwoMdl = true;
        this.trainStructs_partial = trainStructs_partial;
        this.maxRound = maxRound;
        this.seed = seed;
        this.trainStructs_full = trainStructs_full;
        lambda = -1;// 1-model: lambda is inactive
        setModelPath(modelDir,modelNamePrefix);
        multiClassifiers = new MultiClassifiers<>(cls0,lambda,true);
        setDefaultCacheDir();
    }*/

    // 2-model
    /*public CoDLWrapper_LBJ(Learner cls0, List<LearningStruct> trainStructs_partial, int maxRound, int seed, double lambda, String modelDir, String modelNamePrefix) {
        OneMdlOrTwoMdl = false;
        this.trainStructs_partial = trainStructs_partial;
        this.maxRound = maxRound;
        this.seed = seed;
        this.lambda = lambda;
        setModelPath(modelDir,modelNamePrefix);
        multiClassifiers = new MultiClassifiers<>(cls0,lambda,true);
        multiClassifiers.addClassifier(cls0);// this is just a placeholder
        setDefaultCacheDir();
    }*/

    public abstract void loadData_1model() throws Exception;// load trainStructs_partial and trainStructs_full

    public abstract void loadData_2model() throws Exception;// load trainStructs_partial and lambda

    public abstract Learner loadBaseCls() throws Exception;

    public abstract void setCacheDir();// implementation can simply be returning setDefaultCacheDir()

    public abstract LearningStruct inference(LearningStruct doc);//have to make a deep copy of the input

    public abstract Learner learn(List<LearningStruct> slist, int curr_round);

    public abstract String getStructId(LearningStruct st);//used for cache purpose

    protected void setDefaultCacheDir(){
        setCacheDir("serialization/CoDL_cache");
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
        IOUtils.mkdir(this.cacheDir);
    }

    public void setForceUpdate(boolean forceUpdate){
        System.out.printf("[CoDLWrapper_LBJ] forceUpdate=%s\n",forceUpdate);
        this.forceUpdate = forceUpdate;
    }

    private void setModelPath(String modelDir, String modelNamePrefix) {
        IOUtils.mkdir(modelDir);
        this.modelDir = modelDir;
        this.modelNamePrefix = modelNamePrefix;
    }

    public void CoDL(){
        if(!forceUpdate&&modelExists()){
            System.out.println("Model exists. Do not do CoDL. Returning immediately.");
            return;
        }
        for(int iter=0;iter<maxRound;iter++){
            System.out.println("----------------");
            System.out.printf("----Round%d/%d----\n",iter+1,maxRound);
            System.out.println("----------------");
            List<LearningStruct> trainStructs_pseudo_full = new ArrayList<>();
            for(LearningStruct st:trainStructs_partial){
                System.out.printf("----Inference %d/%d\n",trainStructs_partial.indexOf(st)+1,trainStructs_partial.size());
                boolean inferenceNeeded = true;
                if(!forceUpdate&&cacheExists(st,iter)){
                    inferenceNeeded = false;
                    try {
                        String cachepath = cachePath(st, iter);
                        trainStructs_pseudo_full.add((LearningStruct)serializer.deserialize(cachepath));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        System.out.println("[WARNING] failed to load serialized cache in CoDL. Switching to using provided inference function");
                        inferenceNeeded = true;
                    }
                }
                if(inferenceNeeded) {
                    LearningStruct st_inf = inference(st);
                    trainStructs_pseudo_full.add(st_inf);
                    try {
                        serializer.serialize(st_inf, cachePath(st_inf, iter));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        System.out.println("[WARNING] failed to serialize CoDL cache: "+cachePath(st_inf, iter));
                    }
                }
            }

            if(OneMdlOrTwoMdl){// 1-model
                trainStructs_pseudo_full.addAll(trainStructs_full);
            }
            // else 2-model: do nothing
            Learner cls = learn(trainStructs_pseudo_full, iter);
            seed++;
            multiClassifiers.dropClassifier();
            multiClassifiers.addClassifier(cls);
        }
    }

    private String cachePath(LearningStruct st, int iter){
        return String.format("%s%s%s%s_iter%d.ser",cacheDir,File.separator,getStructId(st),OneMdlOrTwoMdl?"_1model":"_2model",iter);
    }

    private boolean cacheExists(LearningStruct st, int iter){
        return IOUtils.isFile(cachePath(st,iter));
    }

    protected boolean modelExists(){
        if(OneMdlOrTwoMdl){
            String mdlPath = modelDir+File.separator+modelNamePrefix+"_1model.lc";
            String lexPath = modelDir+File.separator+modelNamePrefix+"_1model.lex";
            return IOUtils.isFile(mdlPath) && IOUtils.isFile(lexPath);
        }
        else{
            String mdlPath1 = modelDir+File.separator+modelNamePrefix+"_2model_1stCls.lc";
            String lexPath1 = modelDir+File.separator+modelNamePrefix+"_2model_1stCls.lex";
            String mdlPath2 = modelDir+File.separator+modelNamePrefix+"_2model_2ndCls.lc";
            String lexPath2 = modelDir+File.separator+modelNamePrefix+"_2model_2ndCls.lex";
            return new File(mdlPath1).isFile() && new File(lexPath1).isFile()
                    &&new File(mdlPath2).isFile() && new File(lexPath2).isFile();

        }
    }

    public void saveClassifiers(){
        if(OneMdlOrTwoMdl){
            String mdlPath = modelDir+File.separator+modelNamePrefix+"_1model.lc";
            String lexPath = modelDir+File.separator+modelNamePrefix+"_1model.lex";
            multiClassifiers.classifiers.get(0).write(mdlPath,lexPath);
        }
        else{
            String mdlPath = modelDir+File.separator+modelNamePrefix+"_2model_1stCls.lc";
            String lexPath = modelDir+File.separator+modelNamePrefix+"_2model_1stCls.lex";
            multiClassifiers.classifiers.get(0).write(mdlPath,lexPath);

            mdlPath = modelDir+File.separator+modelNamePrefix+"_2model_2ndCls.lc";
            lexPath = modelDir+File.separator+modelNamePrefix+"_2model_2ndCls.lex";
            multiClassifiers.classifiers.get(1).write(mdlPath,lexPath);
        }
    }
}
