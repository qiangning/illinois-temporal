#!/bin/bash
modelname=etTempRelCls
for sent in 0 -1 1
do
	for win in 3 #1 2 3
	do
		for cm in 0 #2 3 4
		do
			mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TemprelPerceptronTrainer_ET -Dexec.args="-d models/tempRel_ET -n $modelname -w $win -s $sent -cm $cm -f 5 -as" > logs/tmp/tempRel_ET/${modelname}_dist${sent}_cls${cm}_win${win}.txt
		done
	done
done