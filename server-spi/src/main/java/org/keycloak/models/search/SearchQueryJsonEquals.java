package org.keycloak.models.search;

import static org.keycloak.models.search.SearchQueryOperator.EQUALS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
@JsonPropertyOrder({ "operator", "property", "value" })
public class SearchQueryJsonEquals extends SearchQueryJson {
	@JsonProperty(required = true)
	private final String property;
	@JsonProperty(required = true)
	private final String value;
	
	@JsonCreator
	public SearchQueryJsonEquals(@JsonProperty("property") String property, @JsonProperty("value") String value) {
		super(EQUALS);
		this.property = property;
		this.value = value;
	}

	public String getProperty() {
		return property;
	}

	public String getValue() {
		return value;
	}
}
