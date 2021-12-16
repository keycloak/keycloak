/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.processor;

import java.io.PrintWriter;
import java.io.Writer;

/**
 *
 * @author hmlnarik
 */
public class PrintWriterNoJavaLang extends PrintWriter {

    public PrintWriterNoJavaLang(Writer out) {
        super(out);
    }

    @Override
    public void println(String x) {
        super.println(x == null ? x : x.replaceAll("java.lang.", ""));
    }

}
