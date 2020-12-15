package org.keycloak.connections.liquibase;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquibase.database.DatabaseFactory;
import liquibase.exception.ServiceNotFoundException;

import liquibase.database.Database;
import liquibase.logging.Logger;
import liquibase.servicelocator.DefaultPackageScanClassResolver;
import liquibase.servicelocator.ServiceLocator;

public class FastServiceLocator extends ServiceLocator {

    private final Map<String, List<String>> services;

    public FastServiceLocator(Map<String, List<String>> services) {
        super(new DefaultPackageScanClassResolver() {
            @Override
            public Set<Class<?>> findImplementations(Class parent, String... packageNames) {
                List<String> found = services.get(parent.getName());

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

        this.services = services;
    }

    @Override
    public Object newInstance(Class requiredInterface) throws ServiceNotFoundException {
        if (Logger.class.equals(requiredInterface)) {
            return new KeycloakLogger();
        }
        return super.newInstance(requiredInterface);
    }

    @Override
    public <T> Class<? extends T>[] findClasses(Class<T> requiredInterface) throws ServiceNotFoundException {
        List<String> found = services.get(requiredInterface.getName());

        if (found == null) {
            return super.findClasses(requiredInterface);
        }

        Set<Class<?>> ret = new HashSet<>();
        for (String i : found) {
            try {
                ret.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return ret.toArray(new Class[ret.size()]);
    }

    public void register(Database database) {
        DatabaseFactory.getInstance().register(database);
    }
}
