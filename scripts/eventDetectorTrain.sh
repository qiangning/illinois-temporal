#!/bin/bash
for win in 2 # 1 2 3
do
	for cm in 0
	do
		mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/eventDetector -n eventPerceptronDetector_repeat -w $win -cm $cm" > logs/tmp/eventDetector_win${win}_clsMode${cm}_repeat.txt
	done
done
# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/eventDetector -n eventPerceptronDetector -w 1 -cm 0" > logs/tmp/eventDetector/eventDetector_win1_clsMode0.txt
# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/tmp3 -n eventPerceptronDetector -w 2 -cm 0" > logs/tmp/eventDetector/eventDetector_win2_clsMode0_allAxes.txt
# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/eventDetector -n eventPerceptronDetector -w 3 -cm 0" > logs/tmp/eventDetector/eventDetector_win3_clsMode0.txt

# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/tmp -n eventPerceptronDetector -w 2 -cm 3" > logs/tmp/eventDetector_win2_clsMode3.txt
# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/tmp -n eventPerceptronDetector -w 2 -cm 4" > logs/tmp/eventDetector_win2_clsMode4.txt
# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.EventAxisPerceptronTrainer -Dexec.args="-d models/tmp -n eventPerceptronDetector -w 2 -cm 5" > logs/tmp/eventDetector_win2_clsMode5.txt



# mvn exec:java -Dexec.mainClass=edu.illinois.cs.cogcomp.temporal.TemporalRelationExtractor.TempRelAnnotator > logs/tmp/GlobalPerformance_eventDetectorImproved.txt
