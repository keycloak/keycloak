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

package org.keycloak.representations.adapters.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of the "global" request (like push notBefore or logoutAll), which is send to all cluster nodes
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GlobalRequestResult {

    private List<String> successRequests;
    private List<String> failedRequests;

    public void addSuccessRequest(String reqUri) {
        if (successRequests == null) {
            successRequests = new ArrayList<>();
        }
        successRequests.add(reqUri);
    }

    public void addFailedRequest(String reqUri) {
        if (failedRequests == null) {
            failedRequests = new ArrayList<>();
        }
        failedRequests.add(reqUri);
    }

    public void addAllSuccessRequests(List<String> reqUris) {
        if (successRequests == null) {
            successRequests = new ArrayList<>();
        }
        successRequests.addAll(reqUris);
    }

    public void addAllFailedRequests(List<String> reqUris) {
        if (failedRequests == null) {
            failedRequests = new ArrayList<>();
        }
        failedRequests.addAll(reqUris);
    }

    public void addAll(GlobalRequestResult merged) {
        if (merged.getSuccessRequests() != null && merged.getSuccessRequests().size() > 0) {
            addAllSuccessRequests(merged.getSuccessRequests());
        }
        if (merged.getFailedRequests() != null && merged.getFailedRequests().size() > 0) {
            addAllFailedRequests(merged.getFailedRequests());
        }
    }

    public List<String> getSuccessRequests() {
        return successRequests;
    }

    public List<String> getFailedRequests() {
        return failedRequests;
    }
}
