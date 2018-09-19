#!/bin/bash

cd "$(dirname "$0")"
. ./common.sh

SAR_OPERATION=${SAR_OPERATION:-stop}

SAR_FOLDER="$PROJECT_BUILD_DIRECTORY/sar"
PID_FILE="$SAR_FOLDER/sar.pid"
TIMESTAMP_FILE="$SAR_FOLDER/sar.timestamp"
if [[ -f "$TIMESTAMP_FILE" ]]; then 
    TIMESTAMP=`cat $TIMESTAMP_FILE`
else
    TIMESTAMP=`date +%s`
fi
SAR_RESULTS_FOLDER="$SAR_FOLDER/$TIMESTAMP"
SAR_OUTPUT_FILE="$SAR_RESULTS_FOLDER/sar-output.bin"
BZIP=${BZIP:-false}
CPU_COUNT=${CPU_COUNT:-`grep -c ^processor /proc/cpuinfo`}

GNUPLOT=${GNUPLOT:-false}
GNUPLOT_SCRIPTS_DIR="$PROJECT_BASEDIR/src/main/gnuplot/sar"
GNUPLOT_COMMON="$GNUPLOT_SCRIPTS_DIR/common.gplot"

function process_cpu_results() {
    RESULTS_FOLDER="$SAR_RESULTS_FOLDER/cpu"; mkdir -p "$RESULTS_FOLDER"
    CPU=${1:-ALL}
    if [ "$CPU" == "ALL" ]; then SAR_PARAMS="-u"; else SAR_PARAMS="-u -P $CPU"; fi
    TXT_FILE="$RESULTS_FOLDER/cpu-$CPU.txt"
    CSV_FILE="${TXT_FILE%.txt}.csv"
    PNG_FILE="${TXT_FILE%.txt}.png"
    sar         $SAR_PARAMS -f   $SAR_OUTPUT_FILE > "$TXT_FILE"
    sadf -d --  $SAR_PARAMS      $SAR_OUTPUT_FILE > "$CSV_FILE"
    if $GNUPLOT; then
        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/cpu.gplot" > "$PNG_FILE"
    fi
}

#function process_net_results() {
#    IFACE=${IFACE:-docker0}
#    RESULTS_FOLDER="$SAR_RESULTS_FOLDER/net"; mkdir -p "$RESULTS_FOLDER"
#    TXT_FILE="$RESULTS_FOLDER/net-$IFACE.txt"
#    CSV_FILE="${TXT_FILE%.txt}.csv"
#    PNG_FILE="${TXT_FILE%.txt}.png"
#    sar         -n DEV -f   $SAR_OUTPUT_FILE > "${TXT_FILE}.tmp"
#    sadf -d --  -n DEV      $SAR_OUTPUT_FILE > "${CSV_FILE}.tmp"
#    head -n 3 "${TXT_FILE}.tmp" > "$TXT_FILE"; grep "$IFACE" "${TXT_FILE}.tmp" >> "$TXT_FILE"; rm "${TXT_FILE}.tmp"
#    head -n 1 "${CSV_FILE}.tmp" > "$CSV_FILE"; grep "$IFACE" "${CSV_FILE}.tmp" >> "$CSV_FILE"; rm "${CSV_FILE}.tmp"
#    if $GNUPLOT; then
#        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/net.gplot" > "$PNG_FILE"
#    fi
#}

function process_io_results() {
    RESULTS_FOLDER="$SAR_RESULTS_FOLDER"
    TXT_FILE="$RESULTS_FOLDER/io.txt"
    CSV_FILE="${TXT_FILE%.txt}.csv"
    sar         -b -f   $SAR_OUTPUT_FILE > "${TXT_FILE}"
    sadf -d --  -b      $SAR_OUTPUT_FILE > "${CSV_FILE}"
    if $GNUPLOT; then
        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/io-requests.gplot" > "${TXT_FILE%.txt}-requests.png"
        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/io-data.gplot" > "${TXT_FILE%.txt}-data.png"
    fi
}

function process_mem_results() {
    RESULTS_FOLDER="$SAR_RESULTS_FOLDER"
    TXT_FILE="$RESULTS_FOLDER/mem.txt"
    CSV_FILE="${TXT_FILE%.txt}.csv"
    PNG_FILE="${TXT_FILE%.txt}.png"
    sar         -r -f   $SAR_OUTPUT_FILE > "${TXT_FILE}"
    sadf -d --  -r      $SAR_OUTPUT_FILE > "${CSV_FILE}"
    if $GNUPLOT; then
        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/mem.gplot" > "$PNG_FILE"
    fi
}

function process_cswch_results() {
    RESULTS_FOLDER="$SAR_RESULTS_FOLDER"
    TXT_FILE="$RESULTS_FOLDER/cswch.txt"
    CSV_FILE="${TXT_FILE%.txt}.csv"
    PNG_FILE="${TXT_FILE%.txt}.png"
    sar         -w -f   $SAR_OUTPUT_FILE > "${TXT_FILE}"
    sadf -d --  -w      $SAR_OUTPUT_FILE > "${CSV_FILE}"
    if $GNUPLOT; then
        gnuplot -e "datafile='$CSV_FILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/cswch.gplot" > "$PNG_FILE"
    fi
}


case "$SAR_OPERATION" in

    start)
        if [[ ! -f "$PID_FILE" ]]; then
            echo "Starting sar command."
            mkdir -p $SAR_RESULTS_FOLDER
            echo $TIMESTAMP > $TIMESTAMP_FILE
            sar -A -o "$SAR_OUTPUT_FILE" 2 &>/dev/null & SAR_PID=$! && echo $SAR_PID > $PID_FILE
        fi
    ;;

    stop)
        if [[ -f "$PID_FILE" ]]; then
            echo "Stopping sar command."
            SAR_PID=`cat $PID_FILE`
            kill $SAR_PID && rm $PID_FILE && rm $TIMESTAMP_FILE

            echo "Processing sar output. GNUPLOT: $GNUPLOT"

            # CPU
            mkdir $SAR_RESULTS_FOLDER/cpu

            process_cpu_results
            for CPU in $(seq -f "%02g" 0 $(( CPU_COUNT-1 )) ); do 
                process_cpu_results $CPU
            done

#            for IFACE in $(ls /sys/class/net); do 
#                process_net_results $IFACE
#            done

            process_io_results 
            process_mem_results 
            process_cswch_results 

            if $BZIP; then bzip2 "$SAR_OUTPUT_FILE"; fi

            echo "Done."
        fi
    ;;

esac
