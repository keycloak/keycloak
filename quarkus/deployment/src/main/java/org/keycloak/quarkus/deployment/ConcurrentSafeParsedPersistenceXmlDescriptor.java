package org.keycloak.quarkus.deployment;

import java.net.URL;
import java.util.List;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

// TODO: remove this workaround when Hibernate ORM ships synchronized access in
//  ParsedPersistenceXmlDescriptor (see https://github.com/keycloak/keycloak/pull/51628)
/**
 * A {@link ParsedPersistenceXmlDescriptor} that synchronizes {@link #getManagedClassNames()} and
 * {@link #addClasses(List)} to prevent concurrent access when one Quarkus build step adds classes
 * while another concurrently iterates the list.
 */
final class ConcurrentSafeParsedPersistenceXmlDescriptor extends ParsedPersistenceXmlDescriptor {

    ConcurrentSafeParsedPersistenceXmlDescriptor(ParsedPersistenceXmlDescriptor original) {
        super(original.getPersistenceUnitRootUrl());
        setName(original.getName());
        if (original.getProviderClassName() != null) {
            setProviderClassName(original.getProviderClassName());
        }
        setTransactionType(original.getPersistenceUnitTransactionType());
        setUseQuotedIdentifiers(original.isUseQuotedIdentifiers());
        setExcludeUnlistedClasses(original.isExcludeUnlistedClasses());
        if (original.getValidationMode() != null) {
            setValidationMode(original.getValidationMode().name());
        }
        if (original.getSharedCacheMode() != null) {
            setSharedCacheMode(original.getSharedCacheMode().name());
        }
        addClasses(original.getManagedClassNames());
        addMappingFiles(original.getMappingFileNames());
        for (URL url : original.getJarFileUrls()) {
            addJarFileUrl(url);
        }
        original.getProperties().forEach((k, v) -> getProperties().setProperty(k.toString(), v.toString()));
    }

    @Override
    public synchronized List<String> getManagedClassNames() {
        return List.copyOf(super.getManagedClassNames());
    }

    @Override
    public synchronized void addClasses(List<String> classes) {
        super.addClasses(classes);
    }

    @Override
    public synchronized void addClasses(String... classes) {
        super.addClasses(classes);
    }
}
