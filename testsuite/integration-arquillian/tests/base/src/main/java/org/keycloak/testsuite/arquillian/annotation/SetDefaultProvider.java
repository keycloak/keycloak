package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SetDefaultProvider {
    String spi();
    String providerId();

    /**
     * <p>Defines whether the default provider should be set by updating an existing Spi configuration.
     *
     * <p>This flag is useful when running the Wildfly distribution and when the server is already configured
     * with a Spi that should only be updated with the default provider.
     *
     * @return {@code true} if the default provider should update an existing Spi configuration. Otherwise, the Spi
     * configuration will be added with the default provider set.
     */
    boolean onlyUpdateDefault() default false;

    /**
     * <p>Defines whether the default provider should be set prior to enabling a feature.
     *
     * <p>This flag should be used together with {@link EnableFeature} so that the default provider
     * is set after enabling a feature. It is useful in case the default provider is not enabled by default,
     * thus requiring the feature to be enabled first.
     *
     * @return {@code true} if the default should be set prior to enabling a feature. Otherwise,
     * the default provider is only set after enabling a feature.
     */
    boolean beforeEnableFeature() default true;
}
