mydir=/home/qning2/Servers/root/shared/preprocessed/qning2/temporal/TBDense
for f in `ls $mydir`
do
	echo $f
	cp /home/qning2/Servers/root/shared/preprocessed/qning2/temporal/illinois-core-utilities_4_0_4/TimeBank/$f /home/qning2/Servers/root/shared/preprocessed/qning2/temporal/illinois-core-utilities_4_0_4/TBDense/$f
done