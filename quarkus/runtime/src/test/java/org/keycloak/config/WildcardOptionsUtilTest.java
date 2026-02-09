package org.keycloak.config;

import org.junit.Test;

import static org.keycloak.config.WildcardOptionsUtil.getWildcardNamedKey;
import static org.keycloak.config.WildcardOptionsUtil.getWildcardPrefix;
import static org.keycloak.config.WildcardOptionsUtil.getWildcardValue;
import static org.keycloak.config.WildcardOptionsUtil.isWildcardOption;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class WildcardOptionsUtilTest {

    @Test
    public void isWildcardOptionTest() {
        assertThat(isWildcardOption("tracing-header-<header>"), is(true));
        assertThat(isWildcardOption("tracing-header-<headxxx"), is(false));
        assertThat(isWildcardOption("tracing-header-headxxx>"), is(false));
        assertThat(isWildcardOption("db-kind-<datasource>"), is(true));
        assertThat(isWildcardOption("http-port"), is(false));
        assertThat(isWildcardOption("quarkus.<sth>.end"), is(true));
        assertThat(isWildcardOption(""), is(false));
        assertThat(isWildcardOption(null), is(false));
    }

    @Test
    public void getWildcardPrefixTest() {
        assertThat(getWildcardPrefix("tracing-header-<header>"), is("tracing-header-"));
        assertThat(getWildcardPrefix("db-kind-<headxxx>"), is("db-kind-"));
        assertThat(getWildcardPrefix("db-kind-<headxxasdfqwer"), is("db-kind-"));
        assertNull(getWildcardPrefix(""));
        assertNull(getWildcardPrefix(null));
    }

    @Test
    public void getWildcardNamedKeyTest() {
        assertThat(getWildcardNamedKey("tracing-header-<header>", "Authorization"), is("tracing-header-Authorization"));
        assertThat(getWildcardNamedKey("db-kind-<datasource>", "user-store"), is("db-kind-user-store"));
        assertThat(getWildcardNamedKey("something-<some>", "something"), is("something-something"));
        assertNull(getWildcardNamedKey("", "something"));
        assertNull(getWildcardNamedKey("something", ""));
        assertNull(getWildcardNamedKey(null, ""));
        assertNull(getWildcardNamedKey("", "null"));
    }

    @Test
    public void getWildcardValueTest() {
        assertThat(getWildcardValue(TracingOptions.TRACING_HEADER, "tracing-header-Authorization"), is("Authorization"));
        assertThat(getWildcardValue(DatabaseOptions.DB_ENABLED_DATASOURCE, "db-enabled-my-store"), is("my-store"));
        assertThat(getWildcardValue(DatabaseOptions.DB_ENABLED_DATASOURCE, "kc.db-enabled-my-store"), is("my-store"));
        assertNull(getWildcardValue(TracingOptions.TRACING_HEADER, "something-wrong"));
        var datasourceKindOption = DatabaseOptions.Datasources.getDatasourceOption(DatabaseOptions.DB).orElseThrow();
        assertThat(getWildcardValue(datasourceKindOption, "db-kind-user"), is("user"));
        assertThat(getWildcardValue(datasourceKindOption, "db-kind-"), is(""));
        assertNull(getWildcardValue(null, "db-kind-"));
        assertNull(getWildcardValue(null, null));
        assertNull(getWildcardValue(TracingOptions.TRACING_HEADER, null));
        assertNull(getWildcardValue(TracingOptions.TRACING_HEADER, ""));
        assertNull(getWildcardValue(TracingOptions.TRACING_HEADER, "null"));
    }
}
