/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email.aws;

import java.util.regex.Pattern;

/**
 * The two things this provider needs to know about an AWS region name: whether it is one, and which
 * DNS suffix its partition uses.
 * <p>
 * Validation is not defensive programming for its own sake. A region name is interpolated into the
 * hostname of the SES and STS endpoints, and both of those requests carry credentials — the STS one
 * carries a service-account token in its body. A region read from the environment as
 * {@code attacker.example/collect} would otherwise build
 * {@code https://sts.attacker.example/collect.amazonaws.com/} and post that token to a host of
 * someone else's choosing.
 */
public final class AwsRegion {

    /** {@code eu-central-1}, {@code us-gov-west-1}, {@code cn-north-1} — and nothing that could smuggle a host or a path. */
    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)+");

    private static final String DEFAULT_DNS_SUFFIX = "amazonaws.com";
    private static final String CHINA_DNS_SUFFIX = "amazonaws.com.cn";

    private AwsRegion() {
    }

    public static boolean isValid(String region) {
        return region != null && VALID.matcher(region).matches();
    }

    /**
     * The DNS suffix of the region's partition. China is the partition that differs for the endpoints
     * this provider builds; the AWS GovCloud and the standard partition share {@code amazonaws.com},
     * and the isolated partitions are not reachable from a server that could run this code anyway.
     */
    public static String dnsSuffix(String region) {
        return region != null && region.startsWith("cn-") ? CHINA_DNS_SUFFIX : DEFAULT_DNS_SUFFIX;
    }
}
