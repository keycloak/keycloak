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

package org.keycloak.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class TestSomething {

    @Test
    public void testHealthEndpoint() throws MalformedURLException, IOException {
        ExecutorService ex = Executors.newCachedThreadPool();
        
        Runnable task = () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:9001/health/ready").openConnection();
                connection.connect();
                System.out.println(connection.getResponseCode());
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        
        Collections.nCopies(6, task).stream().map(CompletableFuture::runAsync).toList().stream().forEach(t -> {
            try {
                t.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        
    }

}
