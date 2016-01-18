package org.keycloak.exportimport;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum Strategy {

    IGNORE_EXISTING,         // Ignore existing user entries
    OVERWRITE_EXISTING       // Overwrite existing user entries
}
