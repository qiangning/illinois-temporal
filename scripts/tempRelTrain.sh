#!/bin/bash
# modelname=eeTempRelClsBugFix
# for sent in -1 # 0 1
# do
# 	for win in 3 #2 1
# 	do
# 		for cm in 0 #2 3 4
# 		do
# 			# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TemprelPerceptronTrainer_EE -Dexec.args="-d models/tempRel/bugfix -n $modelname -w $win -s $sent -cm $cm -as" > logs/tmp/tempRel/bugfix/${modelname}_dist${sent}_cls${cm}_win${win}.txt
# 			mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TemprelPerceptronTrainer_EE -Dexec.args="-d models/tempRel/tbdense/bugfix -n $modelname -w $win -s $sent -cm $cm -train "TBDense_Train_Ser_AutoCorrected,TBDense_Dev_Ser_AutoCorrected,TBDense_Test_Ser_AutoCorrected" -test PLATINUM_Ser_AutoCorrected -f 2 -th "1" -sr "2" -round "200"" > logs/tmp/tempRel/tbdense/bugfix/${modelname}_dist${sent}_cls${cm}_win${win}.txt
# 		done
# 	done
# done

modelname=TCR_eeTempRelCls
for sent in -1 0 1
do
	for win in 3 2 1
	do
		for cm in 0
		do
			# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TemprelPerceptronTrainer_EE -Dexec.args="-d models/tempRel/bugfix -n $modelname -w $win -s $sent -cm $cm -as" > logs/tmp/tempRel/bugfix/${modelname}_dist${sent}_cls${cm}_win${win}.txt
			mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TemprelPerceptronTrainer_EE -Dexec.args="-d models/tempRel/TCR -n $modelname -w $win -s $sent -cm $cm -train "TCR_Train_Ser_AutoCorrected" -test TCR_Test_Ser_AutoCorrected -f 3 -th "1,2" -sr "0.5,1,2" -round "20,50,100"" > logs/tmp/tempRel/TCR/${modelname}_dist${sent}_cls${cm}_win${win}.txt
		done
	done
done