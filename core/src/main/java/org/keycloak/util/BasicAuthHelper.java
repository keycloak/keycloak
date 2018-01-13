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

package org.keycloak.util;

import org.keycloak.common.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BasicAuthHelper
{
    public static String createHeader(String username, String password)
    {
        StringBuffer buf = new StringBuffer(username);
        buf.append(':').append(password);
        try
        {
            return "Basic " + Base64.encodeBytes(buf.toString().getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String[] parseHeader(String header)
    {
        if (header.length() < 6) return null;
        String type = header.substring(0, 5);
        type = type.toLowerCase();
        if (!type.equalsIgnoreCase("Basic")) return null;
        String val = header.substring(6);
        try {
            val = new String(Base64.decode(val.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int seperatorIndex = val.indexOf(":");
        if(seperatorIndex == -1) return null;
        String user = val.substring(0, seperatorIndex);
        String pw = val.substring(seperatorIndex + 1);
        return new String[]{user,pw};
    }
}
