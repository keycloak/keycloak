package org.keycloak.models.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 */
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "operator")
@JsonSubTypes({
	@Type(value = SearchQueryJsonAnd.class, name = "and"),
	@Type(value = SearchQueryJsonOr.class, name = "or"),
	@Type(value = SearchQueryJsonNot.class, name = "not"),
	@Type(value = SearchQueryJsonEquals.class, name = "equals"),
	@Type(value = SearchQueryJsonLike.class, name = "like"),
	@Type(value = SearchQueryJsonIn.class, name = "in"),
	@Type(value = SearchQueryJsonGt.class, name = "gt"),
	@Type(value = SearchQueryJsonGte.class, name = "gte"),
	@Type(value = SearchQueryJsonLt.class, name = "lt"),
	@Type(value = SearchQueryJsonLte.class, name = "lte")
})
public abstract class SearchQueryJson {
	@JsonProperty("operator")
	protected final SearchQueryOperator operator;

	protected SearchQueryJson(SearchQueryOperator operator) {
		this.operator = operator;
	}
	
	public SearchQueryOperator getOperator() {
		return operator;
	}
}
