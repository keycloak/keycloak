set title "Utilisation of Survivor 1 space"
plot for [i in "S1U S1C"] datafile using 1:i title columnheader(i) with lines
