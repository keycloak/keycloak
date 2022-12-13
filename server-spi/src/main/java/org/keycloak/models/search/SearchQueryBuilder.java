package org.keycloak.models.search;

import java.util.Arrays;

public class SearchQueryBuilder {

	private SearchQueryBuilder() {}
	
	public static SearchQueryJson and(SearchQueryJson... values) {
		return new SearchQueryJsonAnd(Arrays.asList(values));
	}
	
	public static SearchQueryJson equals(String property, String value) {
		return new SearchQueryJsonEquals(property, value);
	}
	
	public static SearchQueryJson greaterThan(String property, String value) {
		return new SearchQueryJsonGt(property, value);
	}
	
	public static SearchQueryJson greaterThanOrEquals(String property, String value) {
		return new SearchQueryJsonGte(property, value);
	}
	
	public static SearchQueryJson in(String property, String... values) {
		return new SearchQueryJsonIn(property, Arrays.asList(values));
	}
	
	public static SearchQueryJson like(String property, String value) {
		return new SearchQueryJsonLike(property, value);
	}
	
	public static SearchQueryJson lessThan(String property, String value) {
		return new SearchQueryJsonLt(property, value);
	}
	
	public static SearchQueryJson lessThanOrEquals(String property, String value) {
		return new SearchQueryJsonLte(property, value);
	}
	
	public static SearchQueryJson not(SearchQueryJson value) {
		return new SearchQueryJsonNot(value);
	}
	
	public static SearchQueryJson or(SearchQueryJson... values) {
		return new SearchQueryJsonOr(Arrays.asList(values));
	}	
}
