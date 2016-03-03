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
package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType.EDTChoiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType.EDTDescriptorChoiceType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import java.util.List;

/**
 * Utility for configuration
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 13, 2009
 */
public class CoreConfigUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Get the first metadata descriptor for an IDP
     *
     * @param entitiesDescriptor
     *
     * @return
     */
    public static IDPSSODescriptorType getIDPDescriptor(EntitiesDescriptorType entitiesDescriptor) {
        IDPSSODescriptorType idp = null;
        List<Object> entitiesList = entitiesDescriptor.getEntityDescriptor();
        for (Object theObject : entitiesList) {
            if (theObject instanceof EntitiesDescriptorType) {
                idp = getIDPDescriptor((EntitiesDescriptorType) theObject);
            } else if (theObject instanceof EntityDescriptorType) {
                idp = getIDPDescriptor((EntityDescriptorType) theObject);
            }
            if (idp != null) {
                break;
            }
        }
        return idp;
    }

    /**
     * Get the IDP metadata descriptor from an entity descriptor
     *
     * @param entityDescriptor
     *
     * @return
     */
    public static IDPSSODescriptorType getIDPDescriptor(EntityDescriptorType entityDescriptor) {
        List<EDTChoiceType> edtChoices = entityDescriptor.getChoiceType();
        for (EDTChoiceType edt : edtChoices) {
            List<EDTDescriptorChoiceType> edtDescriptors = edt.getDescriptors();
            for (EDTDescriptorChoiceType edtDesc : edtDescriptors) {
                IDPSSODescriptorType idpSSO = edtDesc.getIdpDescriptor();
                if (idpSSO != null) {
                    return idpSSO;
                }
            }
        }
        return null;
    }

    /**
     * Get the SP Descriptor from an entity descriptor
     *
     * @param entityDescriptor
     *
     * @return
     */
    public static SPSSODescriptorType getSPDescriptor(EntityDescriptorType entityDescriptor) {
        List<EDTChoiceType> edtChoices = entityDescriptor.getChoiceType();
        for (EDTChoiceType edt : edtChoices) {
            List<EDTDescriptorChoiceType> edtDescriptors = edt.getDescriptors();
            for (EDTDescriptorChoiceType edtDesc : edtDescriptors) {
                SPSSODescriptorType spSSO = edtDesc.getSpDescriptor();
                if (spSSO != null) {
                    return spSSO;
                }
            }
        }
        return null;
    }

    /**
     * Given a binding uri, get the IDP identity url
     *
     * @param idp
     * @param bindingURI
     *
     * @return
     */
    public static String getIdentityURL(IDPSSODescriptorType idp, String bindingURI) {
        String identityURL = null;

        List<EndpointType> endpoints = idp.getSingleSignOnService();
        for (EndpointType endpoint : endpoints) {
            if (endpoint.getBinding().toString().equals(bindingURI)) {
                identityURL = endpoint.getLocation().toString();
                break;
            }

        }
        return identityURL;
    }

    /**
     * Given a binding uri, get the IDP identity url
     *
     * @param idp
     * @param bindingURI
     *
     * @return
     */
    public static String getLogoutURL(IDPSSODescriptorType idp, String bindingURI) {
        String logoutURL = null;

        List<EndpointType> endpoints = idp.getSingleLogoutService();
        for (EndpointType endpoint : endpoints) {
            if (endpoint.getBinding().toString().equals(bindingURI)) {
                logoutURL = endpoint.getLocation().toString();
                break;
            }

        }
        return logoutURL;
    }

    /**
     * Given a binding uri, get the IDP logout response url (used for global logouts)
     */
    public static String getLogoutResponseLocation(IDPSSODescriptorType idp, String bindingURI) {
        String logoutResponseLocation = null;

        List<EndpointType> endpoints = idp.getSingleLogoutService();
        for (EndpointType endpoint : endpoints) {
            if (endpoint.getBinding().toString().equals(bindingURI)) {
                if (endpoint.getResponseLocation() != null) {
                    logoutResponseLocation = endpoint.getResponseLocation().toString();
                } else {
                    logoutResponseLocation = null;
                }

                break;
            }

        }
        return logoutResponseLocation;
    }

    /**
     * Get the service url for the SP
     *
     * @param sp
     * @param bindingURI
     *
     * @return
     */
    public static String getServiceURL(SPSSODescriptorType sp, String bindingURI) {
        String serviceURL = null;

        List<IndexedEndpointType> endpoints = sp.getAssertionConsumerService();
        for (IndexedEndpointType endpoint : endpoints) {
            if (endpoint.getBinding().toString().equals(bindingURI)) {
                serviceURL = endpoint.getLocation().toString();
                break;
            }

        }
        return serviceURL;
    }

    private static void addAllEntityDescriptorsRecursively(List<EntityDescriptorType> resultList,
                                                           EntitiesDescriptorType entitiesDescriptorType) {
        List<Object> entities = entitiesDescriptorType.getEntityDescriptor();
        for (Object o : entities) {
            if (o instanceof EntitiesDescriptorType) {
                addAllEntityDescriptorsRecursively(resultList, (EntitiesDescriptorType) o);
            } else if (o instanceof EntityDescriptorType) {
                resultList.add((EntityDescriptorType) o);
            } else {
                throw new IllegalArgumentException("Wrong type: " + o.getClass());
            }
        }
    }
}