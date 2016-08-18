package org.keycloak.models;

/**
 * Denotes an executable Script with metadata.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public interface ScriptModel {

    /**
     * Returns the unique id of the script. {@literal null} for ad-hoc created scripts.
     */
    String getId();

    /**
     * Returns the realm id in which the script was defined.
     */
    String getRealmId();

    /**
     * Returns the name of the script.
     */
    String getName();

    /**
     * Returns the MIME-type if the script code, e.g. for Java Script the MIME-type, {@code text/javascript} is used.
     */
    String getMimeType();

    /**
     * Returns the actual source code of the script.
     */
    String getCode();

    /**
     * Returns the description of the script.
     */
    String getDescription();
}
