package org.keycloak.storage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.keycloak.models.UserModel;

final class FilteredUserQueryBatchIterator implements Iterator<UserModel> {

    private static final int MIN_USER_QUERY_BATCH_SIZE = 100;

    private int batchSize;
    private final BiFunction<Integer, Integer, List<UserModel>> fetchBatch;
    private final Predicate<UserModel> checkPermission;
    private List<UserModel> currentBatch = List.of();
    private int batchIndex;
    private boolean fetched;
    private boolean finished;
    private UserModel nextUser;
    private List<String> lastOffsetZeroBatchIds;
    private final Set<String> filteredUserIds = new HashSet<>();
    private int offset;
    private boolean useOffsets;

    FilteredUserQueryBatchIterator(BiFunction<Integer, Integer, List<UserModel>> fetchBatch, Predicate<UserModel> filter,
                                   Integer firstResult, Integer maxResults) {
        this.fetchBatch = fetchBatch;
        this.checkPermission = filter;
        int skip = firstResult == null || firstResult < 0 ? 0 : firstResult;
        long requestedBatchSize = maxResults == null || maxResults < 0 ? MIN_USER_QUERY_BATCH_SIZE : (long) skip + maxResults;
        this.batchSize = (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_USER_QUERY_BATCH_SIZE, requestedBatchSize));
    }

    Stream<UserModel> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
    }

    @Override
    public boolean hasNext() {
        if (!fetched && !finished) {
            fetchNextAuthorizedUser();
        }
        return fetched;
    }

    @Override
    public UserModel next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        fetched = false;
        return nextUser;
    }

    private void fetchNextAuthorizedUser() {
        while (!finished) {
            if (batchIndex >= currentBatch.size()) {
                currentBatch = fetchNextBatch();
                batchIndex = 0;
                if (currentBatch.isEmpty()) {
                    finished = true;
                    return;
                }
            }

            UserModel entry = currentBatch.get(batchIndex++);
            if (entry != null && filteredUserIds.add(entry.getId()) && checkPermission.test(entry)) {
                nextUser = entry;
                fetched = true;
                return;
            }
        }
    }

    private List<UserModel> fetchNextBatch() {
        if (useOffsets) {
            List<UserModel> batch = fetchBatch.apply(offset, batchSize);
            offset = advanceOffset(offset, batch.size());

            // Null-only batches can represent stale memberships, not exhaustion of later offsets.
            if (isFiltered(batch)) {
                finished = true;
                return List.of();
            }

            return batch;
        }

        List<UserModel> batch = fetchBatch.apply(0, batchSize);
        if (batch.isEmpty()) {
            return batch;
        }

        List<String> batchIds = batch.stream().map(user -> user == null ? null : user.getId()).toList();
        if (lastOffsetZeroBatchIds != null && (lastOffsetZeroBatchIds.equals(batchIds) || hasNoUncheckedUsers(batch))) {
            useOffsets = true;
            offset = batch.size();
            return fetchNextBatch();
        }

        lastOffsetZeroBatchIds = batchIds;
        batchSize = increaseBatchSize(batchSize);
        return batch;
    }

    private boolean hasNoUncheckedUsers(List<UserModel> batch) {
        return batch.stream()
                .filter(Objects::nonNull)
                .allMatch(user -> filteredUserIds.contains(user.getId()));
    }

    private boolean isFiltered(List<UserModel> batch) {
        boolean hasNonNullUser = false;

        for (UserModel user : batch) {
            if (user == null) {
                continue;
            }

            hasNonNullUser = true;

            if (!filteredUserIds.contains(user.getId())) {
                return false;
            }
        }

        return hasNonNullUser;
    }

    private static int increaseBatchSize(int batchSize) {
        return batchSize > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : batchSize * 2;
    }

    private static int advanceOffset(int offset, int batchSize) {
        return offset > Integer.MAX_VALUE - batchSize ? Integer.MAX_VALUE : offset + batchSize;
    }
}
