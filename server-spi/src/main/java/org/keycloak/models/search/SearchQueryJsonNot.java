package org.keycloak.models.search;

import static org.keycloak.models.search.SearchQueryOperator.NOT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
@JsonPropertyOrder({ "operator", "value" })
public class SearchQueryJsonNot extends SearchQueryJson {
	@JsonProperty(required = true)
	private final SearchQueryJson value;
	
	@JsonCreator
	public SearchQueryJsonNot(@JsonProperty("value") SearchQueryJson value) {
		super(NOT);
		this.value = value;
	}

	public SearchQueryJson getValue() {
		return value;
	}
}
