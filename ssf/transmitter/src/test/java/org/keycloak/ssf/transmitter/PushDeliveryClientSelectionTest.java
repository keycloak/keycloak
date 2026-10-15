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
package org.keycloak.ssf.transmitter;

import java.util.Set;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.delivery.push.PrivateAddressDenyingDnsResolver;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator.Reason;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator.SsfPushUrlValidationException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which HTTP client push delivery uses for each combination of the
 * {@code allow-insecure-push-targets} and {@code push-delivery-via-proxy}
 * options, and that delivering through the proxy keeps the push URL checks.
 */
class PushDeliveryClientSelectionTest {

    @Test
    void defaultUsesDirectClientWithAddressCheck() {
        HttpClientProvider httpClientProvider = createDelivery(config(false, false));

        verify(httpClientProvider).createDirectHttpClient(any(PrivateAddressDenyingDnsResolver.class));
        verify(httpClientProvider, never()).getHttpClient();
    }

    @Test
    void pushDeliveryViaProxyUsesSharedClient() {
        HttpClientProvider httpClientProvider = createDelivery(config(false, true));

        verify(httpClientProvider).getHttpClient();
        verify(httpClientProvider, never()).createDirectHttpClient(any());
    }

    @Test
    void allowInsecurePushTargetsUsesSharedClient() {
        HttpClientProvider httpClientProvider = createDelivery(config(true, false));

        verify(httpClientProvider).getHttpClient();
        verify(httpClientProvider, never()).createDirectHttpClient(any());
    }

    @Test
    void pushDeliveryViaProxyKeepsPushUrlChecks() {
        DefaultSsfTransmitterProviderFactory factory = new DefaultSsfTransmitterProviderFactory();
        SsfPushUrlValidator validator = factory.createPushUrlValidator(config(false, true));

        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> validator.validate("https://10.0.0.5/ssf", Set.of("https://10.0.0.5/*")));
        assertEquals(Reason.HOST_PRIVATE, ex.getReason());

        ex = assertThrows(SsfPushUrlValidationException.class,
                () -> validator.validate("http://receiver.example.com/ssf", Set.of("http://receiver.example.com/*")));
        assertEquals(Reason.SCHEME_INSECURE, ex.getReason());
    }

    private static HttpClientProvider createDelivery(SsfTransmitterConfig config) {
        HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
        when(httpClientProvider.createDirectHttpClient(any())).thenReturn(mock(CloseableHttpClient.class));
        when(httpClientProvider.getHttpClient()).thenReturn(mock(CloseableHttpClient.class));
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);

        DefaultSsfTransmitterProviderFactory factory = new DefaultSsfTransmitterProviderFactory() {
            @Override
            protected boolean isOutgoingProxyConfigured() {
                return false;
            }
        };
        SsfTransmitterContext ctx = new SsfTransmitterContext(config, Set.of(), null, s -> null, s -> null, factory);
        factory.createPushDelivery(session, ctx);
        return httpClientProvider;
    }

    private static SsfTransmitterConfig config(boolean allowInsecurePushTargets, boolean pushDeliveryViaProxy) {
        SsfTransmitterConfig defaults = SsfTransmitterConfig.defaults();
        return new SsfTransmitterConfig(
                defaults.getPushEndpointConnectTimeoutMillis(),
                defaults.getPushEndpointSocketTimeoutMillis(),
                SsfTransmitterConfig.DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS,
                SsfTransmitterConfig.DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS,
                SsfTransmitterConfig.DEFAULT_SIGNATURE_ALGORITHM,
                SsfTransmitterConfig.DEFAULT_USER_SUBJECT_FORMAT,
                SsfTransmitterConfig.DEFAULT_DEFAULT_SUBJECTS,
                SsfTransmitterConfig.DEFAULT_SUBJECT_MANAGEMENT_ENABLED,
                SsfTransmitterConfig.DEFAULT_SSE_CAEP_ENABLED,
                SsfTransmitterConfig.DEFAULT_CRITICAL_SUBJECT_MEMBERS,
                SsfTransmitterConfig.DEFAULT_METRICS_ENABLED,
                SsfTransmitterConfig.DEFAULT_SUBJECT_REMOVAL_GRACE_SECONDS,
                allowInsecurePushTargets,
                pushDeliveryViaProxy);
    }
}
