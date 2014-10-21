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
            successRequests = new ArrayList<String>();
        }
        successRequests.add(reqUri);
    }

    public void addFailedRequest(String reqUri) {
        if (failedRequests == null) {
            failedRequests = new ArrayList<String>();
        }
        failedRequests.add(reqUri);
    }

    public void addAllSuccessRequests(List<String> reqUris) {
        if (successRequests == null) {
            successRequests = new ArrayList<String>();
        }
        successRequests.addAll(reqUris);
    }

    public void addAllFailedRequests(List<String> reqUris) {
        if (failedRequests == null) {
            failedRequests = new ArrayList<String>();
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
