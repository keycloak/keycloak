if [ -z "$CPUSETS_DC1" ]; then
    CPUSETS_DC1="2"
fi
if [ -z "$CPUSETS_DC2" ]; then
    CPUSETS_DC2="3"
fi
OUT=docker-compose-crossdc.yml

cat cpuset-scaling/crossdc/base.yml > $OUT
I=0
for CPUSET in $CPUSETS_DC1 ; do
    I=$((I+1))
    sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ cpuset-scaling/crossdc/keycloak_dc1.yml >> $OUT
done
I=0
for CPUSET in $CPUSETS_DC2 ; do
    I=$((I+1))
    sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ cpuset-scaling/crossdc/keycloak_dc2.yml >> $OUT
done
