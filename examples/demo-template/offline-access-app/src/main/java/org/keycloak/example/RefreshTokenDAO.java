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

package org.keycloak.example;

import org.keycloak.common.util.StreamUtil;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Very simple DAO, which stores/loads just one token per whole application into file in tmp directory. Useful just for example purposes.
 * In real environment, token should be stored in database.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RefreshTokenDAO {

    public static final String FILE = System.getProperty("java.io.tmpdir") + "/offline-access-portal";

    public static void saveToken(final String token) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(FILE)));
            writer.print(token);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static String loadToken() throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(FILE);
            return StreamUtil.readString(fis);
        } catch (FileNotFoundException fnfe) {
            return null;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
