package org.keycloak.storage;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.models.UserModel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class FilteredUserQueryBatchIteratorTest {

    @Test
    public void growsProviderLimitUntilPageIsFilled() {
        List<Integer> requestedLimits = new ArrayList<>();
        List<UserModel> users = users(1_000);
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) -> {
            requestedLimits.add(max);
            return users.subList(offset, Math.min(offset + max, users.size()));
        }, user -> Integer.parseInt(user.getId().substring(5)) % 50 == 0, 0, 10);

        List<String> userIds = iterator.stream().limit(10).map(UserModel::getId).toList();

        assertThat(requestedLimits, contains(100, 200, 400, 800));
        assertThat(userIds, contains("user-0", "user-50", "user-100", "user-150", "user-200",
                "user-250", "user-300", "user-350", "user-400", "user-450"));
    }

    @Test
    public void advancesByReturnedBatchSizeWhenProviderCapsResults() {
        List<Integer> requestedOffsets = new ArrayList<>();
        List<UserModel> users = users(6);
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) -> {
            requestedOffsets.add(offset);
            return users.subList(offset, Math.min(offset + 2, users.size()));
        }, user -> true, 0, 5);

        List<String> userIds = iterator.stream().limit(5).map(UserModel::getId).toList();

        assertThat(requestedOffsets, contains(0, 0, 2, 4));
        assertThat(userIds, contains("user-0", "user-1", "user-2", "user-3", "user-4"));
    }

    @Test
    public void progressesAcrossChangingOffsetZeroPagesWithoutDuplicates() {
        List<Integer> offsetZeroFetches = new ArrayList<>();
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) -> {
            if (offset == 0) {
                offsetZeroFetches.add(offset);
                return offsetZeroFetches.size() == 1 ? List.of(user("a"), user("b")) : List.of(user("b"), user("c"));
            }
            return offset == 2 ? List.of(user("d")) : List.of();
        }, user -> true, 0, 10);

        List<String> userIds = iterator.stream().map(UserModel::getId).toList();

        assertThat(userIds, contains("a", "b", "c", "d"));
        assertThat(offsetZeroFetches.size(), is(3));
    }

    @Test
    public void continuesAfterNullOnlyBatches() {
        List<UserModel> staleMemberships = Collections.nCopies(100, null);
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) -> switch (offset) {
            case 0, 100 -> staleMemberships;
            case 200 -> List.of(user("a"));
            default -> List.of();
        }, user -> true, 0, 1);

        assertThat(iterator.stream().map(UserModel::getId).toList(), contains("a"));
    }

    @Test
    public void skipsNullCandidatesBeforePermissionCheck() {
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) ->
                offset == 0 ? Arrays.asList(null, user("a")) : List.of(),
                user -> user.getId().equals("a"), 0, 1);

        assertThat(iterator.stream().map(UserModel::getId).toList(), contains("a"));
    }

    @Test
    public void fetchesProviderResultsOnlyWhenStreamIsConsumed() {
        AtomicInteger fetchCount = new AtomicInteger();
        FilteredUserQueryBatchIterator iterator = new FilteredUserQueryBatchIterator((offset, max) -> {
            fetchCount.incrementAndGet();
            return List.of(user("a"));
        }, user -> true, 0, 1);

        var users = iterator.stream().limit(1);

        assertThat(fetchCount.get(), is(0));
        assertThat(users.map(UserModel::getId).toList(), contains("a"));
        assertThat(fetchCount.get(), is(1));
    }

    private static List<UserModel> users(int count) {
        List<UserModel> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(user("user-" + i));
        }
        return users;
    }

    private static UserModel user(String id) {
        return (UserModel) Proxy.newProxyInstance(UserModel.class.getClassLoader(), new Class<?>[]{UserModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "UserModel[" + id + "]";
                    default -> null;
                });
    }
}
