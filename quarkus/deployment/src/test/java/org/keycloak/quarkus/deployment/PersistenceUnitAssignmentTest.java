package org.keycloak.quarkus.deployment;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link KeycloakProcessor.JpaModelAssignment} and {@link KeycloakProcessor#validateConfiguredPersistenceUnits}.
 */
public class PersistenceUnitAssignmentTest {

    // stands for a server-spi interface: available through the computing index, absent from the plain index
    interface ModelInterface {
    }

    interface OtherInterface extends ModelInterface {
    }

    // implemented by a class of a user persistence unit only
    interface UserUnitInterface {
    }

    @MappedSuperclass
    static class Base implements OtherInterface {
        @Id
        String id;
    }

    @Embeddable
    static class ProviderEmbeddable {
        String value;
    }

    @Converter
    static class ProviderConverter implements jakarta.persistence.AttributeConverter<Boolean, String> {
        @Override
        public String convertToDatabaseColumn(Boolean attribute) {
            return String.valueOf(attribute);
        }

        @Override
        public Boolean convertToEntityAttribute(String dbData) {
            return Boolean.valueOf(dbData);
        }
    }

    @Entity
    static class ProviderEntity extends Base {
        @Embedded
        ProviderEmbeddable embeddable;
        @Convert(converter = ProviderConverter.class)
        Boolean flag;
    }

    static class ProviderId implements Serializable {
        String a;
        String b;
    }

    @Entity
    @IdClass(ProviderId.class)
    static class ProviderIdEntity {
        @Id
        String a;
        @Id
        String b;
    }

    @Entity
    static class ClaimedEntity implements ModelInterface {
        @Id
        String id;
    }

    @Entity
    static class XmlEntity implements UserUnitInterface {
        @Id
        String id;
    }

    private static final String DEFAULT = "<default>";
    private static final String PACKAGE = PersistenceUnitAssignmentTest.class.getPackageName() + ".";
    private static final Class<?>[] INDEXED = { Base.class, ProviderEmbeddable.class, ProviderConverter.class, ProviderEntity.class,
            ProviderId.class, ProviderIdEntity.class, ClaimedEntity.class, XmlEntity.class };

    private static Index plainIndex;
    private static Index computingIndex;

    @BeforeAll
    static void index() throws IOException {
        plainIndex = Index.of(INDEXED);
        Class<?>[] all = new Class<?>[INDEXED.length + 3];
        System.arraycopy(INDEXED, 0, all, 0, INDEXED.length);
        all[INDEXED.length] = ModelInterface.class;
        all[INDEXED.length + 1] = OtherInterface.class;
        all[INDEXED.length + 2] = UserUnitInterface.class;
        computingIndex = Index.of(all);
    }

    private static Map<String, Set<String>> assign(Map<String, Set<String>> userUnitClasses, Map<String, Set<String>> packageRules) {
        return new KeycloakProcessor.JpaModelAssignment(plainIndex, computingIndex, userUnitClasses, packageRules).compute();
    }

    private static String name(Class<?> clazz) {
        return clazz.getName();
    }

    @Test
    public void unclaimedModelClassesBelongToDefaultUnit() {
        Map<String, Set<String>> assignments = assign(Map.of(), Map.of("org.keycloak.models.jpa.entities.", Set.of(DEFAULT)));

        for (Class<?> clazz : INDEXED) {
            assertEquals(Set.of(DEFAULT), assignments.get(name(clazz)), clazz.getName());
        }
        // the class hierarchy of unclaimed classes is assigned with them, java.* types excluded
        assertEquals(Set.of(DEFAULT), assignments.get(name(OtherInterface.class)));
        assertEquals(Set.of(DEFAULT), assignments.get(name(ModelInterface.class)));
        assertEquals(Set.of(DEFAULT), assignments.get(name(UserUnitInterface.class)));
        assertEquals(INDEXED.length + 3, assignments.size(), assignments.toString());
    }

    @Test
    public void claimedModelClassesOnlyGetUnresolvedHierarchyAssigned() {
        Map<String, Set<String>> assignments = assign(Map.of(), Map.of(PACKAGE, Set.of(DEFAULT)));

        assertEquals(Map.of(name(OtherInterface.class), Set.of(DEFAULT), name(ModelInterface.class), Set.of(DEFAULT),
                name(UserUnitInterface.class), Set.of(DEFAULT)), assignments);
    }

    @Test
    public void unresolvedHierarchyAssignedToNamedUnit() {
        Map<String, Set<String>> assignments = assign(Map.of(), Map.of(PACKAGE, Set.of("my-store"), "org.keycloak.models.jpa.entities", Set.of(DEFAULT)));

        assertEquals(Map.of(name(OtherInterface.class), Set.of("my-store"), name(ModelInterface.class), Set.of("my-store"),
                name(UserUnitInterface.class), Set.of("my-store")), assignments);
    }

    @Test
    public void packageRuleMatchesPackagePrefixOnly() {
        // a rule for a package whose name is a prefix of the test package name, but which is not a parent package, claims nothing
        String truncated = PACKAGE.substring(0, PACKAGE.length() - 4);
        assertTrue(name(ProviderEntity.class).startsWith(truncated));
        Map<String, Set<String>> assignments = assign(Map.of(), Map.of(truncated, Set.of("my-store")));

        assertEquals(Set.of(DEFAULT), assignments.get(name(ProviderEntity.class)));
        assertFalse(assignments.values().stream().anyMatch(units -> units.contains("my-store")));
    }

    @Test
    public void classesOfUserPersistenceUnitsAreLeftAlone() {
        Map<String, Set<String>> assignments = assign(Map.of(name(XmlEntity.class), Set.of("xml-unit"), name(ClaimedEntity.class), Set.of("xml-unit"), name(ModelInterface.class), Set.of("xml-unit")), Map.of());

        assertFalse(assignments.containsKey(name(XmlEntity.class)));
        assertFalse(assignments.containsKey(name(ClaimedEntity.class)));
        assertFalse(assignments.containsKey(name(ModelInterface.class)));
        assertEquals(Set.of(DEFAULT), assignments.get(name(ProviderEntity.class)));
        assertEquals(Set.of(DEFAULT), assignments.get(name(OtherInterface.class)));
    }

    @Test
    public void hierarchyOfUserPersistenceUnitClassesIsAssignedToTheirUnit() {
        // Quarkus assigns the classes of a persistence.xml unit, not their hierarchy: an unassigned UserUnitInterface would
        // be reported as a model class without a persistence unit
        Map<String, Set<String>> assignments = assign(Map.of(name(XmlEntity.class), Set.of("xml-unit")), Map.of());

        assertNull(assignments.get(name(XmlEntity.class)));
        assertEquals(Set.of("xml-unit"), assignments.get(name(UserUnitInterface.class)));
    }

    @Test
    public void configuredPersistenceUnitsMustBeNamedAfterTheirDatasource() {
        assertTrue(KeycloakProcessor.validateConfiguredPersistenceUnits(config(Map.of())).isEmpty());
        assertTrue(KeycloakProcessor.validateConfiguredPersistenceUnits(config(Map.of("my-store", Optional.of("my-store")))).isEmpty());

        Optional<String> error = KeycloakProcessor.validateConfiguredPersistenceUnits(config(Map.of("my-store", Optional.of("other"))));
        assertEquals("The persistence unit 'my-store' must use the datasource 'my-store' of the same name, but it uses the datasource 'other'. "
                + "Define the persistence unit of a datasource with the 'kc.db-jpa-packages-my-store' option.", error.orElseThrow());

        error = KeycloakProcessor.validateConfiguredPersistenceUnits(config(Map.of("my-store", Optional.empty())));
        assertEquals("The persistence unit 'my-store' must use the datasource 'my-store' of the same name, but no datasource is set. "
                + "Define the persistence unit of a datasource with the 'kc.db-jpa-packages-my-store' option.", error.orElseThrow());
    }

    private static HibernateOrmConfig config(Map<String, Optional<String>> namedUnits) {
        HibernateOrmConfig config = mock(HibernateOrmConfig.class);
        Map<String, HibernateOrmConfigPersistenceUnit> units = new java.util.HashMap<>();
        namedUnits.forEach((name, datasource) -> {
            HibernateOrmConfigPersistenceUnit unit = mock(HibernateOrmConfigPersistenceUnit.class);
            when(unit.datasource()).thenReturn(datasource);
            units.put(name, unit);
        });
        when(config.namedPersistenceUnits()).thenReturn(units);
        return config;
    }
}
