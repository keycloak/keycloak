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

package org.keycloak.email.aws;

import java.io.IOException;

/**
 * Executes an {@link AwsHttpRequest} exactly as described.
 * <p>
 * "Exactly" is the whole contract. An implementation must not add, remove, reorder or rewrite any
 * header the caller set, must not follow redirects, and must not itself retry a request whose bytes
 * already reached the wire: the first two would break the SigV4 signature, the third would send a
 * transactional email twice, because SES {@code SendEmail} has no idempotency token.
 * <p>
 * The last one is only as strong as the client underneath. {@code KeycloakHttpTransport} runs on the
 * server's shared Apache client, which installs a retry handler of its own and turns it into a
 * resend-after-send when {@code spi-connections-http-client-default-max-retries} is set; that cannot
 * be opted out of per request, so the factory warns at startup when it sees it.
 */
public interface AwsHttpTransport {

    AwsHttpResponse exchange(AwsHttpRequest request) throws IOException;
}
