set datafile separator whitespace
set datafile commentschar ""
set xlabel "Runtime (s)"
set ylabel "Memory (kB)"
set terminal pngcairo size 1280,800
set xtics rotate
set yrange [0:*]
set key below
set grid

set style fill solid 1.0 border -1
set linetype 1 lc rgb '#ff0000'
set linetype 2 lc rgb '#00ff39'
set linetype 3 lc rgb '#4d00ff'
set linetype 4 lc rgb '#ff00fb'
set linetype 5 lc rgb '#00ffff'
set linetype 6 lc rgb '#f7ff00'
