package org.keycloak.quarkus.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.keycloak.connections.jpa.updater.liquibase.lock.DummyLockService;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import liquibase.database.Database;
import liquibase.lockservice.LockService;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.servicelocator.LiquibaseService;
import liquibase.sqlgenerator.SqlGenerator;
import org.keycloak.quarkus.runtime.KeycloakRecorder;

class LiquibaseProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configure(KeycloakRecorder recorder, List<JdbcDataSourceBuildItem> jdbcDataSources, CombinedIndexBuildItem indexBuildItem) {
        DotName liquibaseServiceName = DotName.createSimple(LiquibaseService.class.getName());
        Map<String, List<String>> services = new HashMap<>();
        IndexView index = indexBuildItem.getIndex();
        JdbcDataSourceBuildItem dataSourceBuildItem = jdbcDataSources.get(0);
        String dbKind = dataSourceBuildItem.getDbKind();

        for (Class<?> c : Arrays.asList(liquibase.diff.compare.DatabaseObjectComparator.class,
                liquibase.parser.NamespaceDetails.class,
                liquibase.precondition.Precondition.class,
                Database.class,
                liquibase.change.Change.class,
                liquibase.snapshot.SnapshotGenerator.class,
                liquibase.changelog.ChangeLogHistoryService.class,
                liquibase.datatype.LiquibaseDataType.class,
                liquibase.executor.Executor.class,
                SqlGenerator.class)) {
            List<String> impls = new ArrayList<>();
            services.put(c.getName(), impls);
            Set<ClassInfo> classes = new HashSet<>();
            if (c.isInterface()) {
                classes.addAll(index.getAllKnownImplementors(DotName.createSimple(c.getName())));
            } else {
                classes.addAll(index.getAllKnownSubclasses(DotName.createSimple(c.getName())));
            }
            filterImplementations(c, dbKind, classes);
            for (ClassInfo found : classes) {
                if (Modifier.isAbstract(found.flags()) ||
                        Modifier.isInterface(found.flags()) ||
                        !found.hasNoArgsConstructor() ||
                        !Modifier.isPublic(found.flags())) {
                    continue;
                }
                AnnotationInstance annotationInstance = found.classAnnotation(liquibaseServiceName);
                if (annotationInstance == null || !annotationInstance.value("skip").asBoolean()) {
                    impls.add(found.name().toString());
                }
            }
        }

        services.put(LockService.class.getName(), Arrays.asList(DummyLockService.class.getName()));
        services.put(ChangeLogParser.class.getName(), Arrays.asList(XMLChangeLogSAXParser.class.getName()));

        recorder.configureLiquibase(services);
    }

    private void filterImplementations(Class<?> types, String dbKind, Set<ClassInfo> classes) {
        if (Database.class.equals(types)) {
            // removes unsupported databases
            classes.removeIf(classInfo -> !org.keycloak.quarkus.runtime.storage.database.Database.isLiquibaseDatabaseSupported(classInfo.name().toString(), dbKind));
        }
    }
}
