package org.keycloak.testframework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectAdminClient {

    String ref() default "";

    String realmRef() default "";

    Mode mode() default Mode.BOOTSTRAP;

    String clientRef() default "";

    String userRef() default "";

    enum Mode {

        BOOTSTRAP,
        MANAGED_REALM

    }

}
