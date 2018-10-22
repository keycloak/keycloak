/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

/**
 * @author vramik
 */
public enum ServerSetup {
    FIRST_NODE_IN_FIRST_DC,
    FIRST_NODE_IN_EVERY_DC,
    ALL_NODES_IN_EVERY_DC,
    ALL_NODES_IN_FIRST_DC_FIRST_NODE_IN_SECOND_DC,
    ALL_NODES_IN_FIRST_DC_NO_NODES_IN_SECOND_DC;
}
