/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.header;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jgroups.Header;
import org.jgroups.util.Util;

/**
 * Header which carries an OpenTelemetry {@link io.opentelemetry.api.trace.Span} between requests and responses
 *
 * @author Bela Ban
 * @since 1.0.0
 */
public class TracerHeader extends Header {
    public static final short ID = 1050;
    protected final Map<String, String> ctx = new HashMap<>();

    public TracerHeader() {
    }

    public short getMagicId() {
        return ID;
    }

    public Supplier<? extends Header> create() {
        return TracerHeader::new;
    }

    public void put(String key, String value) {
        ctx.put(key, value);
    }

    public String get(String key) {
        return ctx.get(key);
    }

    public Set<String> keys() {
        return ctx.keySet();
    }

    public int serializedSize() {
        int size = Integer.BYTES;
        int num_attrs = ctx.size();
        if (num_attrs > 0) {
            for (Map.Entry<String, String> entry : ctx.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                size += Util.size(key) + Util.size(val);
            }
        }
        return size;
    }

    public void writeTo(DataOutput out) throws IOException {
        out.writeInt(ctx.size());
        if (!ctx.isEmpty()) {
            for (Map.Entry<String, String> e : ctx.entrySet()) {
                Util.writeString(e.getKey(), out);
                Util.writeString(e.getValue(), out);
            }
        }
    }

    public void readFrom(DataInput in) throws IOException {
        int size = in.readInt();
        if (size > 0) {
            for (int i = 0; i < size; i++)
                ctx.put(Util.readString(in), Util.readString(in));
        }
    }

    public String toString() {
        return ctx.toString();
    }
}
