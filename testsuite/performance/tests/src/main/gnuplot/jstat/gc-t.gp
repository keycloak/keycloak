set ylabel "GC time (s)"
set title "Utilisation of Garbage collection time (young, full, total)"
plot for [i in "YGCT FGCT GCT"] datafile using 1:i title columnheader(i) with lines
