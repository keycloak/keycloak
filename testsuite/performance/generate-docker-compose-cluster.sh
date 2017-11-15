if [ -z "$CPUSETS" ]; then
    CPUSETS="2-3"
fi
OUT=docker-compose-cluster.yml

cat cpuset-scaling/cluster/base.yml > $OUT
I=0
for CPUSET in $CPUSETS ; do
    I=$((I+1))
    sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ cpuset-scaling/cluster/keycloak.yml >> $OUT
done
