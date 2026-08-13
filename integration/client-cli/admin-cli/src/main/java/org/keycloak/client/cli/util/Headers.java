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
package org.keycloak.client.cli.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.apache.http.entity.ContentType;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class Headers implements Iterable<Header> {

    private LinkedHashMap<String, Header> headers = new LinkedHashMap<>();

    public void add(String header, String value) {
        headers.put(header.toLowerCase(), new Header(header, value));
    }

    public boolean addIfMissing(String header, String value) {
        String key = header.toLowerCase();
        if (!headers.containsKey(key)) {
            headers.put(key, new Header(header, value));
            return true;
        }
        return false;
    }

    public boolean contains(String header) {
        String key = header.toLowerCase();
        return headers.containsKey(key);
    }

    public Header get(String header) {
        return headers.get(header.toLowerCase());
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.values().iterator();
    }

    public Optional<ContentType> getContentType() {
        return Optional.ofNullable(headers.get("content-type")).map(Header::getValue).map(ContentType::parse);
    }
}
