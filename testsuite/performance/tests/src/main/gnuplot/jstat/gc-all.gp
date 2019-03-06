set title "Heap + Non-heap Memory"
plot\
    datafile using 1:(column('S0C')+column('S1C')+column('EC')+column('OC')+column('MC')+column('CCSC')) title 'CCSC' with filledcurves x1, \
    datafile using 1:(column('S0C')+column('S1C')+column('EC')+column('OC')+column('MC')) title 'MC' with filledcurves x1, \
    datafile using 1:(column('S0C')+column('S1C')+column('EC')+column('OC')) title 'OC' with filledcurves x1, \
    datafile using 1:(column('S0C')+column('S1C')+column('EC')) title 'EC' with filledcurves x1, \
    datafile using 1:(column('S0C')+column('S1C')) title 'S1C' with filledcurves x1, \
    datafile using 1:'S0C' title 'S0C' with filledcurves x1
