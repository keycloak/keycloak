package org.keycloak.connections.liquibase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquibase.exception.ServiceNotFoundException;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.keycloak.connections.jpa.updater.liquibase.lock.CustomInsertLockRecordGenerator;
import org.keycloak.connections.jpa.updater.liquibase.lock.CustomLockDatabaseChangeLogGenerator;
import org.keycloak.connections.jpa.updater.liquibase.lock.DummyLockService;

import liquibase.database.Database;
import liquibase.lockservice.LockService;
import liquibase.logging.Logger;
import liquibase.parser.ChangeLogParser;
import liquibase.servicelocator.DefaultPackageScanClassResolver;
import liquibase.servicelocator.LiquibaseService;
import liquibase.servicelocator.ServiceLocator;
import liquibase.sqlgenerator.SqlGenerator;

public class FastServiceLocator extends ServiceLocator {

    private static Map<String, List<String>> CLASS_INDEX = new HashMap<>();

    static {
        DotName liquibaseServiceName = DotName.createSimple(LiquibaseService.class.getName());

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/liquibase.idx")) {
            IndexReader reader = new IndexReader(in);
            Index index = reader.read();
            for (Class<?> c : Arrays.asList(liquibase.diff.compare.DatabaseObjectComparator.class,
                    liquibase.parser.NamespaceDetails.class,
                    liquibase.precondition.Precondition.class,
                    Database.class,
                    ChangeLogParser.class,
                    liquibase.change.Change.class,
                    liquibase.snapshot.SnapshotGenerator.class,
                    liquibase.changelog.ChangeLogHistoryService.class,
                    liquibase.datatype.LiquibaseDataType.class,
                    liquibase.executor.Executor.class,
                    LockService.class,
                    SqlGenerator.class)) {
                List<String> impls = new ArrayList<>();
                CLASS_INDEX.put(c.getName(), impls);
                Set<ClassInfo> classes = new HashSet<>();
                if (c.isInterface()) {
                    classes.addAll(index.getAllKnownImplementors(DotName.createSimple(c.getName())));
                } else {
                    classes.addAll(index.getAllKnownSubclasses(DotName.createSimple(c.getName())));
                }
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
        } catch (IOException cause) {
            throw new RuntimeException("Failed to get liquibase jandex index", cause);
        }

        CLASS_INDEX.put(Logger.class.getName(), Arrays.asList(KeycloakLogger.class.getName()));
        CLASS_INDEX.put(LockService.class.getName(), Arrays.asList(DummyLockService.class.getName()));
        CLASS_INDEX.put(ChangeLogParser.class.getName(), Arrays.asList(XMLChangeLogSAXParser.class.getName()));
        CLASS_INDEX.get(SqlGenerator.class.getName()).add(CustomInsertLockRecordGenerator.class.getName());
        CLASS_INDEX.get(SqlGenerator.class.getName()).add(CustomLockDatabaseChangeLogGenerator.class.getName());
    }

    protected FastServiceLocator() {
        super(new DefaultPackageScanClassResolver() {
            @Override
            public Set<Class<?>> findImplementations(Class parent, String... packageNames) {
                List<String> found = CLASS_INDEX.get(parent.getName());

                if (found == null) {
                    return super.findImplementations(parent, packageNames);
                }

                Set<Class<?>> ret = new HashSet<>();
                for (String i : found) {
                    try {
                        ret.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return ret;
            }
        });

        if (!System.getProperties().containsKey("liquibase.scan.packages")) {
            if (getPackages().remove("liquibase.core")) {
                addPackageToScan("liquibase.core.xml");
            }

            if (getPackages().remove("liquibase.parser")) {
                addPackageToScan("liquibase.parser.core.xml");
            }

            if (getPackages().remove("liquibase.serializer")) {
                addPackageToScan("liquibase.serializer.core.xml");
            }

            getPackages().remove("liquibase.ext");
            getPackages().remove("liquibase.sdk");
        }

        // we only need XML parsers
        getPackages().remove("liquibase.parser.core.yaml");
        getPackages().remove("liquibase.serializer.core.yaml");
        getPackages().remove("liquibase.parser.core.json");
        getPackages().remove("liquibase.serializer.core.json");
    }

    @Override
    public Object newInstance(Class requiredInterface) throws ServiceNotFoundException {
        if (Logger.class.equals(requiredInterface)) {
            return new KeycloakLogger();
        }
        return super.newInstance(requiredInterface);
    }

    public void register(Class<? extends Database> type) {
        CLASS_INDEX.put(Database.class.getName(), Arrays.asList(type.getName()));
    }
}
