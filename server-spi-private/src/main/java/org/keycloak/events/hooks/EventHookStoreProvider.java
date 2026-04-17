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

package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;

public interface EventHookStoreProvider extends Provider {

    default Stream<EventHookTargetModel> getTargetsStream(String realmId) {
        return getTargetsStream(realmId, null);
    }

    Stream<EventHookTargetModel> getTargetsStream(String realmId, Boolean enabled);

    EventHookTargetModel getTarget(String realmId, String targetId);

    EventHookTargetModel createTarget(EventHookTargetModel target);

    EventHookTargetModel updateTarget(EventHookTargetModel target);

    boolean deleteTarget(String realmId, String targetId);

    default List<EventHookTargetModel> getEnabledTargets(String realmId) {
        return getTargetsStream(realmId, true).toList();
    }

    void createMessages(List<EventHookMessageModel> messages);

    Stream<EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, Integer first, Integer max);

    EventHookMessageModel getMessage(String realmId, String messageId);

    default Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, Integer first, Integer max) {
        return getLogsStream(realmId, messageId, targetId, null, null, null, first, max);
    }

    default Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
            String executionId, String search, Integer first, Integer max) {
        String sourceType = null;
        String event = null;
        String client = null;
        String user = null;
        String ipAddress = null;
        String resourceType = null;
        String resourcePath = null;
        String status = null;
        String messageStatus = null;
        Long dateFrom = null;
        Long dateTo = null;

        return getLogsStream(realmId, messageId, targetId, targetType, sourceType, event, client, user, ipAddress,
                resourceType, resourcePath, status, messageStatus, dateFrom, dateTo, executionId, search, first, max);
    }

    Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
            String sourceType, String event, String client, String user, String ipAddress,
            String resourceType, String resourcePath, String status, String messageStatus,
            Long dateFrom, Long dateTo, String executionId, String search,
            Integer first, Integer max);

    Map<String, EventHookTargetStatus> getLatestTargetStatuses(String realmId);

    List<EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner);

    default List<EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner,
            List<String> targetTypes) {
        return claimAvailableMessages(maxResults, now, staleClaimBefore, claimOwner);
    }

    List<EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
            long staleClaimBefore, String claimOwner);

    default Long getPendingAggregationDeadline(String realmId, String targetId, long now) {
        return null;
    }

    boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore);

    EventHookMessageModel updateMessage(EventHookMessageModel message);

    void createLog(EventHookLogModel log);
}
