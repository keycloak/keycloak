package org.keycloak.models.search;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
public enum SearchQueryOperator {
	AND,
	EQUALS,
	IN,
	LIKE,
	NOT,
	OR,
	GT,
	GTE,
	LT,
	LTE;
	
	@JsonValue
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}