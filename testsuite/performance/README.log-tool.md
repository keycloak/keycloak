Example of using log-tool.sh
----------------------------

Perform the usual test run:

```
mvn verify -Pteardown
mvn verify -Pprovision
mvn verify -Pgenerate-data -Ddataset=100users -Dimport.workers=10 -DhashIterations=100
mvn verify -Ptest -Ddataset=100users -DusersPerSec=5 -DrampUpPeriod=10 -DuserThinkTime=0 -DbadLoginAttempts=1 -DrefreshTokenCount=1 -DmeasurementPeriod=60
```

Now analyze the generated simulation.log (adjust LOG_DIR, FROM, and TO):

```
LOG_DIR=$HOME/devel/keycloak/keycloak/testsuite/performance/tests/target/gatling/keycloaksimulation-1502735555123
```

Get general statistics about the run to help with deciding about the interval to extract:
```
tests/log-tool.sh -s -f $LOG_DIR/simulation.log 
tests/log-tool.sh -s -f $LOG_DIR/simulation.log --lastRequest "Browser logout"
```

Set start and end times for the extraction, and create new directory for results:
```
FROM=1502735573285
TO=1502735581063

RESULT_DIR=tests/target/gatling/keycloaksimulation-$FROM\_$TO

mkdir $RESULT_DIR
```

Extract a portion of the original log, and inspect statistics of resulting log:
```
tests/log-tool.sh -f $LOG_DIR/simulation.log -o $RESULT_DIR/simulation-$FROM\_$TO.log -e --start $FROM --end $TO 

tests/log-tool.sh -f $RESULT_DIR/simulation-$FROM\_$TO.log -s
```

Generate another set of reports from extracted log: 
```
GATLING_HOME=$HOME/devel/gatling-charts-highcharts-bundle-2.1.7

cd $GATLING_HOME
bin/gatling.sh -ro $RESULT_DIR

```


Installing Gatling Highcharts 2.1.7
-----------------------------------

```
git clone http://github.com/gatling/gatling
cd gatling
git checkout v2.1.7
git checkout -b v2.1.7
sbt clean compile
sbt publishLocal publishM2
cd ..

git clone http://github.com/gatling/gatling-highcharts
cd gatling-highcharts/
git checkout v2.1.7
git checkout -b v2.1.7
sbt clean compile
sbt publishLocal publishM2
cd ..

unzip ~/.ivy2/local/io.gatling.highcharts/gatling-charts-highcharts-bundle/2.1.7/zips/gatling-charts-highcharts-bundle-bundle.zip
cd gatling-charts-highcharts-bundle-2.1.7

bin/gatling.sh
```


