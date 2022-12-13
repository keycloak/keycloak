package org.keycloak.models.search;

import static org.keycloak.models.search.SearchQueryOperator.AND;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
@JsonPropertyOrder({ "operator", "values" })
public class SearchQueryJsonAnd extends SearchQueryJson {
	@JsonProperty(required = true)
	private final List<SearchQueryJson> values;
	
	@JsonCreator
	public SearchQueryJsonAnd(@JsonProperty("values") List<SearchQueryJson> values) {
		super(AND);
		this.values = values;
	}
	
	public List<SearchQueryJson> getValues() {
		return values;
	}
}
