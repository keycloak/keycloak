/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.provider;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration property metadata.  Used to render generic configuration pages for Keycloak extensions in the admin console.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProviderConfigProperty {
    public static final String BOOLEAN_TYPE="boolean";
    public static final String STRING_TYPE="String";

    /**
     * Possibility to configure multiple String values of any value (something like "redirect_uris" for clients)
     */
    public static final String MULTIVALUED_STRING_TYPE="MultivaluedString";

    public static final String SCRIPT_TYPE="Script";
    public static final String FILE_TYPE="File";
    public static final String ROLE_TYPE="Role";

    /**
     * Possibility to configure single String value, which needs to be chosen from the list of predefined values (HTML select)
     */
    public static final String LIST_TYPE="List";

    /**
     * Possibility to configure multiple String values, which needs to be chosen from the list of predefined values (HTML select with multiple)
     */
    public static final String MULTIVALUED_LIST_TYPE="MultivaluedList";

    public static final String CLIENT_LIST_TYPE="ClientList";
    public static final String PASSWORD="Password";

    /**
     * textarea field
     */
    public static final String TEXT_TYPE="Text";

    /**
     * Configure multiple (key, value) pairs
     */
    public static final String MAP_TYPE ="Map";

    public static final String USER_PROFILE_ATTRIBUTE_LIST_TYPE="UserProfileAttributeList";

    protected String name;
    protected String label;
    protected String helpText;
    protected String type = STRING_TYPE;
    protected Object defaultValue;
    protected List<String> options;
    protected boolean secret;

    public ProviderConfigProperty() {
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue) {
        this.name = name;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue, String... options) {
        this.name = name;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = Arrays.asList(options);
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue, boolean secret) {
        this(name, label, helpText, type, defaultValue);
        this.secret = secret;
    }

    /**
     * Name of the config variable stored in the database
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Label shown in the admin console when configuring the variable
     *
     * @return
     */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Type of the variable.  i.e. boolean, string etc.  See the constants declared in this class for what your choices
     * are.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Default value for the variable
     *
     * @return
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * For list types, this is a list of choices to choose from.
     *
     * @return
     */
    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    /**
     * Help text that will be displayed in the admin console tooltip
     *
     * @return
     */
    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    /**
     * If true, this variable is only writeable.  It will never be viewable.  This is important for things like
     * passwords in which you never want to display them on the screen.
     *
     * @return
     */
    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

}
