package org.keycloak.scim.protocol.request;

import org.keycloak.scim.filter.ScimFilterParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SearchRequestTest {

    @Test
    public void testGetFilterContextReturnsNullWhenNoFilter() {
        SearchRequest request = new SearchRequest();
        assertNull(request.getFilterContext());
    }

    @Test
    public void testGetFilterContextReturnsNullWhenBlankFilter() {
        SearchRequest request = new SearchRequest();
        request.setFilter("  ");
        assertNull(request.getFilterContext());
    }

    @Test
    public void testGetFilterContextParsesValidFilter() {
        SearchRequest request = new SearchRequest();
        request.setFilter("userName eq \"john\"");
        ScimFilterParser.FilterContext ctx = request.getFilterContext();
        assertNotNull(ctx);
        assertNotNull(ctx.expression());
    }

    @Test
    public void testGetFilterContextCachesResult() {
        SearchRequest request = new SearchRequest();
        request.setFilter("userName eq \"john\"");
        ScimFilterParser.FilterContext first = request.getFilterContext();
        ScimFilterParser.FilterContext second = request.getFilterContext();
        assertSame(first, second);
    }

    @Test
    public void testSetFilterInvalidatesCache() {
        SearchRequest request = new SearchRequest();
        request.setFilter("userName eq \"john\"");
        ScimFilterParser.FilterContext first = request.getFilterContext();

        request.setFilter("displayName co \"doe\"");
        ScimFilterParser.FilterContext second = request.getFilterContext();
        assertNotNull(second);
        assertNotSame(first, second);
    }

    @Test
    public void testSetFilterToNullClearsCache() {
        SearchRequest request = new SearchRequest();
        request.setFilter("userName eq \"john\"");
        assertNotNull(request.getFilterContext());

        request.setFilter(null);
        assertNull(request.getFilterContext());
    }

    @Test
    public void testBuilderPreservesFilterContextCaching() {
        SearchRequest request = SearchRequest.builder()
                .withFilter("userName eq \"john\"")
                .build();
        ScimFilterParser.FilterContext first = request.getFilterContext();
        ScimFilterParser.FilterContext second = request.getFilterContext();
        assertSame(first, second);
    }

}
