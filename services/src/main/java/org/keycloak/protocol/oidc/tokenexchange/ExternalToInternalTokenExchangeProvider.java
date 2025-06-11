/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.tokenexchange;

import jakarta.ws.rs.core.Response;
import org.keycloak.protocol.oidc.TokenExchangeContext;

/**
 * Provider for external-internal token exchange
 *
 * TODO Should not extend from V1TokenExchangeProvider, but rather AbstractTokenExchangeProvider or from StandardTokenExchangeProvider (as issuing internal tokens might be done in a same/similar way like for standard V2 provider)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExternalToInternalTokenExchangeProvider extends V1TokenExchangeProvider {

    @Override
    public boolean supports(TokenExchangeContext context) {
        return (isExternalInternalTokenExchangeRequest(context));
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    protected Response tokenExchange() {
        String subjectToken = context.getParams().getSubjectToken();
        String subjectTokenType = context.getParams().getSubjectTokenType();
        String subjectIssuer = getSubjectIssuer(this.context, subjectToken, subjectTokenType);
        return exchangeExternalToken(subjectIssuer, subjectToken);
    }

}
