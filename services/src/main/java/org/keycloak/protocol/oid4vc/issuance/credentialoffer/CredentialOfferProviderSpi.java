/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class CredentialOfferProviderSpi implements Spi {
    @Override public String getName() { return "credential-offer-provider"; }
    @Override public Class<CredentialOfferProvider> getProviderClass() { return CredentialOfferProvider.class; }
    @Override public Class<CredentialOfferProviderFactory> getProviderFactoryClass() { return CredentialOfferProviderFactory.class; }
    @Override public boolean isInternal() { return true; }
}
