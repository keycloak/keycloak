set title "Utilisation of Old space"
plot for [i in "OU OC"] datafile using 1:i title columnheader(i) with lines
