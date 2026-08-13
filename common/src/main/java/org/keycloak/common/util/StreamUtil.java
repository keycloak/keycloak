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

package org.keycloak.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class StreamUtil {

    private static final int BUFFER_LENGTH = 4096;

    private StreamUtil() {
    }

    /**
     * Reads string from byte input stream.
     * @param in InputStream to build the String from
     * @return String representation of the input stream contents decoded using default charset
     * @throws IOException
     * @deprecated Use {@link #readString(java.io.InputStream, java.nio.charset.Charset)} variant.
     */
    @Deprecated
    public static String readString(InputStream in) throws IOException
    {
        return readString(in, Charset.defaultCharset());
    }

    /**
     * Reads string from byte input stream.
     * @param in InputStream to build the String from
     * @param charset Charset used to decode the input stream
     * @return String representation of the input stream contents decoded using given charset
     * @throws IOException
     */
    public static String readString(InputStream in, Charset charset) throws IOException
    {
        char[] buffer = new char[BUFFER_LENGTH];
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        int wasRead;
        do
        {
            wasRead = reader.read(buffer, 0, BUFFER_LENGTH);
            if (wasRead > 0)
            {
                builder.append(buffer, 0, wasRead);
            }
        }
        while (wasRead > -1);

        return builder.toString();
    }
}
