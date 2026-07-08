package org.keycloak.admin.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListOptionsTest {

    private enum TestField implements SortField {
        FOO,
        BAR;

        @Override
        public String getApiName() {
            return name().toLowerCase();
        }

        static Optional<TestField> fromApiName(String apiName) {
            return Stream.of(values()).filter(field -> field.getApiName().equals(apiName)).findFirst();
        }
    }

    @Test
    void testGetFieldsEmpty() {
        ListOptions options = new ListOptions();
        options.setFields(Set.of());
        assertEquals("", options.fields);
        assertEquals(Set.of(), options.getFields());
    }
    
    @Test
    void testGetFields() {
        ListOptions options = new ListOptions();
        options.fields = "a,b";
        assertEquals(Set.of("a", "b"), options.getFields());
    }

    @Test
    void testGetSortSegmentsEmpty() {
        ListOptions options = new ListOptions();
        options.setSort(List.<SortOption<TestField>>of());
        assertEquals("", options.sort);
        assertEquals(List.of(), options.getSortSegments());
    }

    @Test
    void testGetSortSegments() {
        ListOptions options = new ListOptions();
        options.sort = "bar|desc,foo";
        assertEquals(List.of(new SortSegment("bar", SortOrder.DESC), new SortSegment("foo", SortOrder.ASC)),
                options.getSortSegments());
    }

    @Test
    void getSortSegmentsCachesParsedResult() {
        ListOptions options = new ListOptions();
        options.sort = "bar|desc,foo";

        List<SortSegment> first = options.getSortSegments();
        List<SortSegment> second = options.getSortSegments();

        assertEquals(List.of(new SortSegment("bar", SortOrder.DESC), new SortSegment("foo", SortOrder.ASC)), first);
        assertSame(first, second);
    }

    @Test
    void testGetSortResolvesFields() {
        ListOptions options = new ListOptions();
        options.sort = "bar|desc,foo";
        assertEquals(List.of(SortOption.of(TestField.BAR, SortOrder.DESC), SortOption.of(TestField.FOO)),
                options.getSort(TestField::fromApiName));
    }

    @Test
    void testSetSort() {
        ListOptions options = new ListOptions();
        options.setSort(List.of(SortOption.of(TestField.FOO, SortOrder.DESC)));
        assertEquals("foo|desc", options.sort);
    }

    @Test
    void invalidSortSegmentFormatThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.sort = "|desc";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::getSortSegments);
        assertEquals("sort must specify at least one field", exception.getMessage());
    }

    @Test
    void testSortTime() {
        ListOptions options = new ListOptions();
        options.setSort(List.of(SortOption.of(ClientField.CREATED_TIMESTAMP, SortOrder.DESC)));
        assertEquals("createdTimestamp|desc", options.sort);
    }

    @Test
    void invalidSortFieldThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.sort = "unknown";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> options.getSort(TestField::fromApiName));
        assertEquals("unknown is not a sortable field", exception.getMessage());
    }

    @Test
    void invalidSortDirectionThrowsBadRequest() {
        ListOptions options = new ListOptions();
        options.sort = "foo|what";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, options::getSortSegments);
        assertEquals("sort direction must be asc or desc", exception.getMessage());
    }

}
