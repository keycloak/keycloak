#!/bin/bash -eu

# Build Keycloak

## Maven build arguments
MAVEN_ARGS="-Djavac.src.version=17 -Djavac.target.version=17 "
MAVEN_ARGS=$MAVEN_ARGS"-DskipTests -Dgpg.skip -Dmaven.source.skip "
MAVEN_ARGS=$MAVEN_ARGS"-DskipExamples -DskipTestsuite -DskipQuarkus"

## Exclude unfuzzed modules
## This is needed to decrease the build time by excluding modules
## which are not used by the fuzzers.
EXCLUDE_DOCS="!docs,!docs/maven-plugin,!docs/guides"

EXCLUDE_DEPENDENCY="!dependencies/server-all"

EXCLUDE_FEDERATION="!federation,!federation/kerberos,!federation/ldap,!federation/sssd"

EXCLUDE_INTEGRATION="!integration,!integration/admin-client-jee,!integration/admin-client,"
EXCLUDE_INTEGRATION=$EXCLUDE_INTEGRATION"!integration/client-registration,!integration/client-cli,"
EXCLUDE_INTEGRATION=$EXCLUDE_INTEGRATION"!integration/client-cli/client-registration-cli,"
EXCLUDE_INTEGRATION=$EXCLUDE_INTEGRATION"!integration/client-cli/admin-cli,!integration/client-cli/client-cli-dist"

EXCLUDE_JS="!js,!js/apps/account-ui,!js/apps/admin-ui,!js/libs/keycloak-admin-client,!js/libs/keycloak-js"

EXCLUDE_MISC="!misc,!misc/keycloak-test-helper,!misc/spring-boot-starter,!misc/spring-boot-starter/keycloak-spring-boot-starter"

EXCLUDE_MODEL="!model/legacy-services,!model/infinispan,!model/map-jpa,"
EXCLUDE_MODEL=$EXCLUDE_MODEL"!model/map-hot-rod,!model/map-ldap,!model/map-file"

EXCLUDE_QUARKUS="!quarkus,!quarkus/config-api,!quarkus/runtime,!quarkus/deployment,"
EXCLUDE_QUARKUS=$EXCLUDE_QUARKUS"!quarkus/server,!quarkus/dist,!quarkus/tests,!quarkus/tests/junit5"

EXCLUDE_REST="!rest,!rest/admin-ui-ext"

EXCLUDE_SERVICE="!services"

EXCLUDE_MODULE=$EXCLUDE_DOCS,$EXCLUDE_DEPENDENCY,$EXCLUDE_FEDERATION,$EXCLUDE_INTEGRATION,$EXCLUDE_JS
EXCLUDE_MODULE=$EXCLUDE_MODULE,$EXCLUDE_MISC,$EXCLUDE_MODEL,$EXCLUDE_QUARKUS,$EXCLUDE_REST

## Activate shade plugin
## This is needed to activate the shade plugin to combine all needed dependencies and build classes
## for each module into a single jar. This limit the maximum number of jars and exempt the need
## to handle separate module dependencies. The limiting action of the maximum number of jars is needed
## to avoid "Arguments too long" error in bash execution of oss-fuzz.
PLUGIN="<plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId>"
PLUGIN=$PLUGIN"<version>\${shade.plugin.version}</version><executions><execution><phase>package</phase>"
PLUGIN=$PLUGIN"<goals><goal>shade</goal></goals><configuration><filters><filter><artifact>*:*</artifact>"
PLUGIN=$PLUGIN"<excludes><exclude>META-INF/*.SF</exclude><exclude>META-INF/*.DSA</exclude>"
PLUGIN=$PLUGIN"<exclude>META-INF/*.RSA</exclude></excludes></filter></filters></configuration>"
PLUGIN=$PLUGIN"</execution></executions></plugin></plugins><pluginManagement>"
sed -i "s#<pluginManagement>#$PLUGIN#g" ./pom.xml

## Execute maven build
$MVN clean package -pl "$EXCLUDE_MODULE" $MAVEN_ARGS -Dfuzzing
CURRENT_VERSION=$($MVN org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
 -Dexpression=project.version -q -DforceStdout)

cp "fuzzing/target/keycloak-fuzzing-$CURRENT_VERSION.jar" $OUT/keycloak-fuzzing.jar

ALL_JARS="keycloak-fuzzing.jar"

# The classpath at build-time includes the project jars in $OUT as well as the
# Jazzer API.
BUILD_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "$OUT/%s:"):$JAZZER_API_PATH

# All .jar and .class files lie in the same directory as the fuzzer at runtime.
RUNTIME_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "\$this_dir/%s:"):\$this_dir

for fuzzer in $(find $SRC/keycloak/fuzzing -name '*Fuzzer.java'); do
  fuzzer_basename=$(basename -s .java $fuzzer)

  # Create an execution wrapper that executes Jazzer with the correct arguments.
  echo "#!/bin/bash
# LLVMFuzzerTestOneInput for fuzzer detection.
this_dir=\$(dirname \"\$0\")
if [[ \"\$@\" =~ (^| )-runs=[0-9]+($| ) ]]; then
  mem_settings='-Xmx1900m:-Xss900k'
else
  mem_settings='-Xmx2048m:-Xss1024k'
fi

apt install openjdk-17-jdk -y

export JAVA_HOME=\"/usr/lib/jvm/java-17-openjdk-amd64\"
export LD_LIBRARY_PATH=\"\$JAVA_HOME/lib/server\":\$this_dir
export PATH=\$JAVA_HOME/bin:\$PATH
export TARGET_PACKAGE_PREFIX=org.keycloak.*

CURRENT_JAVA_VERSION=\$(java --version | head -n1)

if [[ \"\$CURRENT_JAVA_VERSION\" != \"openjdk 17\"* ]]
then
  echo Requires JDK-17+, found \$CURRENT_JAVA_VERSION
  exit -1
fi

\$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \
--cp=$RUNTIME_CLASSPATH \
--target_class=$fuzzer_basename \
--jvm_args=\"\$mem_settings\" \
\$@" > $OUT/$fuzzer_basename
  chmod u+x $OUT/$fuzzer_basename
done

zip $OUT/SamlParserFuzzer_seed_corpus.zip $SRC/keycloak/fuzzing/src/main/resources/SamlParserFuzzer_seed1
zip $OUT/JwkParserFuzzer_seed_corpus.zip $SRC/keycloak/fuzzing/src/main/resources/JwkParserFuzzer_seed_1
zip $OUT/JoseParserFuzzer_seed_corpus.zip $SRC/keycloak/fuzzing/src/main/resources/json.seed
cp $SRC/keycloak/fuzzing/src/main/resources/json.dict $OUT/JwkParserFuzzer.dict
cp $SRC/keycloak/fuzzing/src/main/resources/json.dict $OUT/JoseParserFuzzer.dict
