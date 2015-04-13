package org.keycloak.federation.ldap.idm.model;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a property of an IdentityType, Partition or Relationship as being an attribute of that
 * IdentityType, Partition or Relationship.
 *
 * @author Shane Bryzak
 */
@Target({METHOD, FIELD})
@Documented
@Retention(RUNTIME)
@Inherited
public @interface AttributeProperty {

    /**
     * <p>Managed properties are stored as ad-hoc attributes and mapped from and to a specific property of a type.</p>
     *
     * @return
     */
    boolean managed() default false;

}
