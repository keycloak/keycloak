package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EnableCiba {

    ;

    enum PROVIDER_ID {

        DELEGATE_DECOUPLED_AUTHN("delegate-decoupled-authn",
                new String[] {
                        "/subsystem=keycloak-server/spi=decoupled-authn/provider=delegate-decoupled-authn/:add(enabled=true, " +
                            "properties={decoupledAuthnRequestUri => \"https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/request-decoupled-authentication\"})"},
                    new String[] {});

        final String name;
        final String[] cliInstallationCommands;
        final String[] cliRemovalCommands;

        PROVIDER_ID(final String name, final String[] cliInstallationCommands, final String[] cliRemovalCommands) {
            this.name = name;
            this.cliInstallationCommands = cliInstallationCommands;
            this.cliRemovalCommands = cliRemovalCommands;
        }

        public String getName() {
            return this.name;
        }

        public String[] getCliInstallationCommands() {
            return this.cliInstallationCommands;
        }

        public String[] getCliRemovalCommands() {
            return this.cliRemovalCommands;
        }
    };

    PROVIDER_ID providerId() default PROVIDER_ID.DELEGATE_DECOUPLED_AUTHN;
}
