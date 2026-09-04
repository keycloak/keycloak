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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * An {@link AwsHttpTransport} that answers from a queue of canned responses and records every
 * request it was given, so a test can assert on the exact bytes that would have reached the wire.
 */
public final class FakeTransport implements AwsHttpTransport {

    private final Deque<Object> responses = new ArrayDeque<>();
    private final List<AwsHttpRequest> requests = new ArrayList<>();

    public FakeTransport respondWith(int status, String body) {
        return respondWith(status, Map.of(), body);
    }

    public FakeTransport respondWith(int status, Map<String, String> headers, String body) {
        responses.add(new AwsHttpResponse(status, headers, body.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public FakeTransport failWith(IOException failure) {
        responses.add(failure);
        return this;
    }

    @Override
    public AwsHttpResponse exchange(AwsHttpRequest request) throws IOException {
        requests.add(request);
        Object response = responses.poll();
        if (response == null) {
            throw new AssertionError("Unexpected request, no response queued: " + request.method() + " " + request.uri());
        }
        if (response instanceof IOException failure) {
            throw failure;
        }
        return (AwsHttpResponse) response;
    }

    public List<AwsHttpRequest> requests() {
        return List.copyOf(requests);
    }

    public AwsHttpRequest lastRequest() {
        if (requests.isEmpty()) {
            throw new AssertionError("No request was made");
        }
        return requests.get(requests.size() - 1);
    }

    public AwsHttpRequest request(int index) {
        return requests.get(index);
    }

    public int requestCount() {
        return requests.size();
    }
}
