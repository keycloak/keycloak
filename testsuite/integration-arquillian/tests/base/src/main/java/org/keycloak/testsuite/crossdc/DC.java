/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.crossdc;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Identifier of datacentre in the testsuite
 * @author hmlnarik
 */
public enum DC {
    FIRST,
    SECOND,
    UNDEFINED;

    public int getDcIndex() {
        return ordinal();
    }

    private static final DC[] VALID_DCS = new DC[] { FIRST, SECOND };

    public static Stream<DC> validDcsStream() {
        return Arrays.stream(VALID_DCS);
    }
}
