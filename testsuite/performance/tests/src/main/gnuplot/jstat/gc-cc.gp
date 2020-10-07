set title "Utilisation of Compressed Classes space"
plot for [i in "CCSU CCSC"] datafile using 1:i title columnheader(i) with lines
