set title "Utilisation of Survivor 0 space"
plot for [i in "S0U S0C"] datafile using 1:i title columnheader(i) with lines
