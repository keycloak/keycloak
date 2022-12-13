package org.keycloak.models.search;

import static org.keycloak.models.search.SearchQueryOperator.IN;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
@JsonPropertyOrder({ "operator", "property", "values" })
public class SearchQueryJsonIn extends SearchQueryJson {
	@JsonProperty(required = true)
	private final String property;
	@JsonProperty(required = true)
	private final List<String> values;
	
	@JsonCreator
	public SearchQueryJsonIn(@JsonProperty("property") String property, @JsonProperty("values") List<String> values) {
		super(IN);
		this.property = property;
		this.values = values;
	}

	public String getProperty() {
		return property;
	}

	public List<String> getValues() {
		return values;
	}
}
