package org.keycloak.testframework.scim.client.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectScimClient {

    String clientId() default "scim-client";
    String clientSecret() default "secret";

    /**
     * Attach to an existing client instead of creating one; when attaching to an existing client the config will be ignored
     * and the client will not be deleted automatically.
     *
     * @return the client-id of the existing client to attach to
     */
    String attachTo() default "";
}
