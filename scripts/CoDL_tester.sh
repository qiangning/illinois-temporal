#!/bin/bash
for lambda in 1
do
	for sd in 33 34 35 36 #37 38 39 40 41
	do
		for sr in 0.1 0.3 0.5 0.7 0.9 0.2 0.4 0.6 0.8 1.0
		do
			for sm in 0 3
			do
				max=1
				echo lambda $lambda sd $sd sr $sr sm $sm maxIter $max

				#local
				# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.explorations.ideas.CoDL_Tester -Dexec.args="-d models/tempRel/tbdense/CoDL -n eeTempRelCls -sr $sr -o -r -sm $sm"> logs/tmp/tempRel/tbdense/CoDL/eeTempRelCls_sr${sr}_win3_1mdl_local_resp_sm${sm}.txt

				#global
				#hard
				# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.explorations.ideas.CoDL_Tester -Dexec.args="-d models/tempRel/tbdense/CoDL -n eeTempRelCls -sr $sr -o -i -r -h -sm $sm"> logs/tmp/tempRel/tbdense/CoDL/eeTempRelCls_sr${sr}_win3_1mdl_global_resp_hard_sm${sm}.txt

				#1model
				mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.CoDL_PartialVsFull_FixNoDoc -Dexec.args="-d models/tempRel/tbdense/CoDL_PinBase -n eeTempRelClsNoVagSam -sr $sr -o -i -r -sm $sm -sd $sd -f -max $max -train_p "TimeBank_Minus_TBDense_Ser_AutoCorrected" "> logs/tmp/tempRel/tbdense/CoDL_PinBase/seed${sd}/eeTempRelClsNoVagSam_sr${sr}_win3_1mdl_global_resp_soft_sm${sm}_sd${sd}_max${max}.txt

				#2model
				# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.explorations.ideas.CoDL_PartialVsFull_FixNoDoc -Dexec.args="-d models/tempRel/tbdense/CoDL_PinBase -n eeTempRelClsNoVagSam -sr $sr -lambda $lambda -i -r -sm $sm -sd $sd -f"> logs/tmp/tempRel/tbdense/CoDL_PinBase/seed${sd}/eeTempRelClsNoVagSam_sr${sr}_win3_2mdl_global_resp_soft_lambda${lambda}_sm${sm}_sd${sd}.txt
			done
		done
	done
done