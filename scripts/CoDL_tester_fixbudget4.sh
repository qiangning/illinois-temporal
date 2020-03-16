#!/bin/bash
max=1
for sd in $(seq 9 16)
do
	for budget in 2000
	do
		for sr in 0.1 0.3 0.5 0.7 0.9 0.2 0.4 0.6 0.8 1.0
		do
			echo budget $budget sd $sd sr $sr maxIter $max
			mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.CoDL_PartialVsFull_FixNoTemprel -Dexec.args="-b $budget -d models/tempRel/tbdense/CoDL_PinBase_FixBudget_AQinP_shuffle -n eeTempRelClsNoVagSam -sr $sr -o -i -r -sd $sd -f -max $max -train_p "TimeBank_Minus_TBDense_Ser_AutoCorrected,AQUAINT_Ser_AutoCorrected" "> logs/tmp/tempRel/tbdense/CoDL_PinBase_FixBudget_AQinP_shuffle/seed${sd}/eeTempRelClsNoVagSam_budget${budget}_sr${sr}_win3_1mdl_global_resp_soft_sm${sm}_sd${sd}_max${max}.txt
		done
	done
done