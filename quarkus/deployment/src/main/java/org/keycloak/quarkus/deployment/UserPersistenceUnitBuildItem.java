package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

/**
 * A persistence unit defined by a user in a {@code META-INF/persistence.xml} of a provider. It keeps the datasource
 * name Keycloak resolved for the unit (the {@code <datasource>} part of {@code db-*-<datasource>} options) and the
 * multi-tenancy strategy the unit requested through {@code hibernate.multiTenancy}.
 */
final class UserPersistenceUnitBuildItem extends MultiBuildItem {

    private final String unitName;
    private final String dataSourceName;
    private final MultiTenancyStrategy multiTenancyStrategy;

    UserPersistenceUnitBuildItem(String unitName, String dataSourceName, MultiTenancyStrategy multiTenancyStrategy) {
        this.unitName = unitName;
        this.dataSourceName = dataSourceName;
        this.multiTenancyStrategy = multiTenancyStrategy;
    }

    String getUnitName() {
        return unitName;
    }

    String getDataSourceName() {
        return dataSourceName;
    }

    MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }
}
