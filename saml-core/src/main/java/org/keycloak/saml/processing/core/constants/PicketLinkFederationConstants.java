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
package org.keycloak.saml.processing.core.constants;

/**
 * Constants useful to the JBoss Identity Federation project
 *
 * @author Anil.Saldhana@redhat.com
 * @since Feb 23, 2009
 */
public interface PicketLinkFederationConstants {

    /**
     * Flag to indicate whether JAXB Schema Validation is turned on
     */
    String JAXB_SCHEMA_VALIDATION = "org.picketlink.jaxb.schema.validation";

    String SCHEMA_IDFED = "schema/config/picketlink-fed.xsd";
    String SCHEMA_IDFED_HANDLER = "schema/config/picketlink-fed-handler.xsd";
    String DSA_SIGNATURE_ALGORITHM = "SHA1withDSA";
    String RSA_SIGNATURE_ALGORITHM = "SHA1withRSA";

    // File Based Meta data Configuration Constants
    String SERIALIZATION_EXTENSION = ".SER";
    String FILE_STORE_DIRECTORY = "/picketlink-store";
    String IDP_PROPERTIES = "/identityproviders.properties";
    String SP_PROPERTIES = "/serviceproviders.properties";

    String SALT = "salt";
    String ITERATION_COUNT = "iterationCount";

    String PBE_ALGORITHM = "PBEwithMD5andDES";
    // Prefix to indicate a particular configuration property value is masked
    String PASS_MASK_PREFIX = "MASK-";

    RuntimePermission RUNTIME_PERMISSION_CORE = new RuntimePermission("org.picketlink.core");

}