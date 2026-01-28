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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PublicKeysWrapper {

    private final List<KeyWrapper> keys;
    private final Long expirationTime;

    public static final PublicKeysWrapper EMPTY = new PublicKeysWrapper(Collections.emptyList());

    public PublicKeysWrapper(List<KeyWrapper> keys) {
        this(keys, null);
    }

    public PublicKeysWrapper(List<KeyWrapper> keys, Long expirationTime) {
        this.keys = keys;
        this.expirationTime = expirationTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
     }

    public List<KeyWrapper> getKeys() {
        return keys;
    }

    public List<String> getKids() {
        return keys.stream()
                .map(KeyWrapper::getKid)
                .collect(Collectors.toList());
    }

    /**
     * Find an appropriate key given a KID and algorithm.
     * Prefer matching on both parameters, but may partially match on KID only. Or if KID is not provided, the
     * algorithm. Will use a flagged default client certificate otherwise, if a match is not found.
     * @param kid rfc7517 KID parameter
     * @param alg rfc7517 alg parameter
     * @return {@link KeyWrapper} matching given parameters
     */
    public KeyWrapper getKeyByKidAndAlg(String kid, String alg) {

        Stream<KeyWrapper> potentialMatches = Stream.concat(
            keys.stream().filter(keyWrapper -> Objects.equals(kid, keyWrapper.getKid()) && Objects.equals(alg, keyWrapper.getAlgorithm())),
            keys.stream().filter(keyWrapper -> Objects.equals(kid, keyWrapper.getKid())));

        if (kid == null) {
            potentialMatches = Stream.of(
                    potentialMatches,
                    keys.stream().filter(keyWrapper -> Objects.equals(alg, keyWrapper.getAlgorithmOrDefault())),
                    keys.stream().filter(KeyWrapper::isDefaultClientCertificate)
                ).flatMap(i -> i);
        }

        return potentialMatches.findFirst().orElse(null);
    }

    /**
     * Returns the first key that matches the predicate.
     * @param predicate The predicate
     * @return The first key that matches the predicate or null
     */
    public KeyWrapper getKeyByPredicate(Predicate<KeyWrapper> predicate) {
        return keys.stream().filter(predicate).findFirst().orElse(null);
    }
}
