/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.saml;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.testframework.https.ManagedCertificates;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SamlClientBuilder {

    private final List<SamlClient.Step> steps = new LinkedList<>();
    private ManagedCertificates certificates;

    public SamlClientBuilder() {
    }

    public SamlClientBuilder(ManagedCertificates certificates) {
        this.certificates = certificates;
    }

    public SamlClientBuilder certificates(ManagedCertificates certificates) {
        this.certificates = certificates;
        return this;
    }

    public SamlClient execute() {
        return execute(resp -> {});
    }

    public SamlClient execute(Consumer<CloseableHttpResponse> resultConsumer) {
        final SamlClient samlClient = createSamlClient();
        samlClient.executeAndTransform(r -> {
            resultConsumer.accept(r);
            return null;
        }, steps);
        return samlClient;
    }

    protected SamlClient createSamlClient() {
        return new SamlClient(certificates);
    }

    public <T> T executeAndTransform(SamlClient.ResultExtractor<T> resultTransformer) {
        return createSamlClient().executeAndTransform(resultTransformer, steps);
    }

    public List<SamlClient.Step> getSteps() {
        return steps;
    }

    public <T extends SamlClient.Step> T addStepBuilder(T step) {
        steps.add(step);
        return step;
    }

    public SamlClientBuilder addStep(SamlClient.Step step) {
        steps.add(step);
        return this;
    }

    public SamlClientBuilder doNotFollowRedirects() {
        return addStep(new SamlClient.DoNotFollowRedirectStep());
    }

    public SamlClientBuilder followOneRedirect() {
        return doNotFollowRedirects()
                .addStep((client, currentURI, currentResponse, context) -> {
                    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, currentResponse.getStatusLine().getStatusCode());
                    String location = currentResponse.getFirstHeader("Location").getValue();
                    return new HttpGet(currentURI.resolve(location));
                });
    }

    public SamlClientBuilder navigateTo(String httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> new HttpGet(httpGetUri));
        return this;
    }

    public SamlClientBuilder navigateTo(URI httpGetUri) {
        steps.add((client, currentURI, currentResponse, context) -> new HttpGet(httpGetUri));
        return this;
    }

    public LoginBuilder login() {
        return addStepBuilder(new LoginBuilder(this));
    }

    public ModifySamlResponseStepBuilder processSamlResponse(SamlClient.Binding responseBinding) {
        if (responseBinding == SamlClient.Binding.REDIRECT) {
            doNotFollowRedirects();
        }
        return addStepBuilder(new ModifySamlResponseStepBuilder(responseBinding, this));
    }
}
