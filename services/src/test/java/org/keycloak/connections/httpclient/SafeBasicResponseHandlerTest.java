/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Schwartz
 */
public class SafeBasicResponseHandlerTest {

    @Test
    public void shouldThrowExceptionForLongResponses() throws UnsupportedEncodingException {
        // arrange
        AtomicBoolean inputStreamHasBeenClosed = new AtomicBoolean(false);
        HttpEntity entity = new StringEntity("1234567890") {
            @Override
            public InputStream getContent() throws IOException {
                InputStream delegate = super.getContent();
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return delegate.read();
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        inputStreamHasBeenClosed.set(true);
                    }
                };
            }
        };

        // act
        IOException exception = Assert.assertThrows(IOException.class, () -> new SafeBasicResponseHandler(5).handleEntity(entity));

        // assert
        MatcherAssert.assertThat("Too long response should throw an exception", exception.getMessage(), Matchers.startsWith("Response is at least"));
        MatcherAssert.assertThat("Stream should have been closed", inputStreamHasBeenClosed.get(), Matchers.is(true));
    }

}