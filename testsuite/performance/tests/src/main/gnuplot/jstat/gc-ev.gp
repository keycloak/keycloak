set ylabel "Number of events"
set title "Utilisation of Garbage collection events (young, full)"
plot for [i in "YGC FGC"] datafile using 1:i title columnheader(i) with lines
