/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.crypto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PublicKeysWrapper {

    private final List<KeyWrapper> keys;

    public static final PublicKeysWrapper EMPTY = new PublicKeysWrapper(Collections.emptyList());

    public PublicKeysWrapper(List<KeyWrapper> keys) {
        this.keys = keys;
    }

    public List<KeyWrapper> getKeys() {
        return keys;
    }

    public List<String> getKids() {
        return keys.stream()
                .map(KeyWrapper::getKid)
                .collect(Collectors.toList());
    }

    public KeyWrapper getKeyByKidAndAlg(String kid, String alg) {
        return keys.stream()
                .filter(keyWrapper -> kid == null || kid.equals(keyWrapper.getKid()))
                .filter(keyWrapper -> alg == null || alg.equals(keyWrapper.getAlgorithmOrDefault()) || (keyWrapper.getAlgorithm() == null && kid != null))
                .findFirst().orElse(null);
    }
}
