package org.keycloak.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CollectionUtilTest {

    @Test
    public void joinInputNoneOutputEmpty() {
        final ArrayList<String> strings = new ArrayList<>();
        final String retval = CollectionUtil.join(strings, ",");
        Assertions.assertEquals("", retval);
    }

    @Test
    public void joinInput2SeparatorNull() {
        final ArrayList<String> strings = new ArrayList<>();
        strings.add("foo");
        strings.add("bar");
        final String retval = CollectionUtil.join(strings, null);
        Assertions.assertEquals("foonullbar", retval);
    }

    @Test
    public void joinInput1SeparatorNotNull() {
        final ArrayList<String> strings = new ArrayList<>();
        strings.add("foo");
        final String retval = CollectionUtil.join(strings, ",");
        Assertions.assertEquals("foo", retval);
    }

  @Test
  public void joinInput2SeparatorNotNull() {
    final ArrayList<String> strings = new ArrayList<>();
    strings.add("foo");
    strings.add("bar");
    final String retval = CollectionUtil.join(strings, ",");
    Assertions.assertEquals("foo,bar", retval);
  }

  @Test
  public void testEmptyCollection() {
    List<String> list = new ArrayList<>();

    assertThat(CollectionUtil.isEmpty(list), is(true));
    assertThat(CollectionUtil.isNotEmpty(list), is(false));

    list.add("something");

    assertThat(CollectionUtil.isEmpty(list), is(false));
    assertThat(CollectionUtil.isNotEmpty(list), is(true));

    Set<Object> set = new HashSet<>();

    assertThat(CollectionUtil.isEmpty(set), is(true));
    assertThat(CollectionUtil.isNotEmpty(set), is(false));

    set.add("something");

    assertThat(CollectionUtil.isEmpty(set), is(false));
    assertThat(CollectionUtil.isNotEmpty(set), is(true));
  }

    @Test
    public void equalsCollectionTest() {
        Assertions.assertFalse(CollectionUtil.collectionEquals(Arrays.asList(1, 3, 2), Arrays.asList(1, 3)));
        Assertions.assertFalse(CollectionUtil.collectionEquals(Arrays.asList("A", "C"), Arrays.asList("A", "C", "B")));
        Assertions.assertFalse(CollectionUtil.collectionEquals(Arrays.asList(1, 3, 2, 3), Arrays.asList(1, 2, 3, 2)));
        Assertions.assertTrue(CollectionUtil.collectionEquals(Arrays.asList(1, 3, 3), Arrays.asList(3, 1, 3)));
    }

    @Nested
    class PartitionTest {

        // Note: the common module is compiled at Java 8 level, hence no Stream.toList() / List.of() here.
        private List<String> ids(int n) {
            return IntStream.range(0, n).mapToObj(i -> "id-" + i).collect(Collectors.toList());
        }

        @Test
        void splitsIntoConsecutiveChunksWithPartialTail() {
            List<List<Integer>> chunks = CollectionUtil.partition(Arrays.asList(1, 2, 3, 4, 5), 2);
            Assertions.assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5)), chunks);
        }

        // Exhaustive sweep with small chunk sizes: catches boundary interactions
        // (n == cs, n == cs +/- 1, n == k*cs, n == k*cs +/- 1) that single examples can mask.
        @Test
        void exhaustiveSweepForSmallChunkSizes() {
            for (int cs = 1; cs <= 7; cs++) {
                for (int n = 0; n <= 200; n++) {
                    List<String> original = ids(n);
                    List<List<String>> chunks = CollectionUtil.partition(original, cs);

                    // count
                    Assertions.assertEquals((n + cs - 1) / cs, chunks.size(), "count: n=" + n + " cs=" + cs);
                    // reconstruction: nothing lost, nothing duplicated, order preserved
                    List<String> rebuilt = new ArrayList<>(n);
                    chunks.forEach(rebuilt::addAll);
                    Assertions.assertEquals(original, rebuilt, "reconstruction: n=" + n + " cs=" + cs);
                    // sizes: every chunk non-empty, only the last may be partial
                    for (int i = 0; i < chunks.size(); i++) {
                        int size = chunks.get(i).size();
                        Assertions.assertTrue(size > 0 && size <= cs, "size: n=" + n + " cs=" + cs + " chunk=" + i);
                        if (i < chunks.size() - 1) {
                            Assertions.assertEquals(cs, size, "fullness: n=" + n + " cs=" + cs + " chunk=" + i);
                        }
                    }
                }
            }
        }

        @Test
        void acceptsAnyCollectionAndPreservesIterationOrder() {
            Set<String> source = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
            List<List<String>> chunks = CollectionUtil.partition(source, 2);
            Assertions.assertEquals(Arrays.asList(Arrays.asList("c", "a"), Arrays.asList("b")), chunks);
        }

        @Test
        void chunksAreIndependentCopiesNotSubListViews() {
            // subList views become invalid if the backing list is structurally modified;
            // the chunks must not be affected by that.
            List<String> source = new ArrayList<>(ids(5));
            List<List<String>> chunks = CollectionUtil.partition(source, 2);

            source.clear(); // structural modification of the original

            List<String> rebuilt = new ArrayList<>();
            Assertions.assertDoesNotThrow(() -> chunks.forEach(rebuilt::addAll),
                    "chunks must not be live views of the source list");
            Assertions.assertEquals(ids(5), rebuilt);
        }

        @Test
        void duplicateInputElementsArePreserved() {
            // partition must not deduplicate; that is the caller's concern
            List<String> withDupes = Arrays.asList("a", "b", "a", "c", "b");
            List<String> rebuilt = new ArrayList<>();
            CollectionUtil.partition(withDupes, 2).forEach(rebuilt::addAll);
            Assertions.assertEquals(withDupes, rebuilt);
        }

        @Test
        void emptyOrNullInputYieldsNoChunks() {
            Assertions.assertTrue(CollectionUtil.partition(Collections.emptyList(), 3).isEmpty());
            Assertions.assertTrue(CollectionUtil.partition(null, 3).isEmpty());
        }

        @Test
        void rejectsNonPositiveChunkSize() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> CollectionUtil.partition(Arrays.asList(1, 2), 0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> CollectionUtil.partition(Arrays.asList(1, 2), -1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> CollectionUtil.partition(Collections.emptyList(), 0));
        }
    }
}
