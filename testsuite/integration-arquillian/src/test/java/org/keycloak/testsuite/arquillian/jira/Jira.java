/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Value should contain name of the issue listed in JBoss JIRA (like
 * KEYCLOAK-1234), it can also contain multiple names separated by coma.
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Jira {

	String value() default "";
}
