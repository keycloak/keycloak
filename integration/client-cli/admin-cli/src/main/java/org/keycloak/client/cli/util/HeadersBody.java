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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.entity.ContentType;

import static org.keycloak.client.cli.util.IoUtil.copyStream;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HeadersBody {

    private Headers headers;
    private InputStream body;


    public HeadersBody(Headers headers) {
        this.headers = headers;
    }

    public HeadersBody(Headers headers, InputStream body) {
        this.headers = headers;
        this.body = body;
    }

    public Headers getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public String readBodyString() {
        byte [] buffer = readBodyBytes();
        return new String(buffer, getContentCharset());
    }

    public byte[] readBodyBytes() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyStream(getBody(), os);
        return os.toByteArray();
    }

    public Charset getContentCharset() {
        return headers.getContentType().map(ContentType::getCharset).orElseGet(() -> Charset.forName("iso-8859-1"));
    }

}
