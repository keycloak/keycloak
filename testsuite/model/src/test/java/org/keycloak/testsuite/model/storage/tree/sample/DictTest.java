package org.keycloak.testsuite.model.storage.tree.sample;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;

import org.junit.Test;
import org.keycloak.models.map.client.MapClientEntity;

public class DictTest {
    @Test
    public void testDictClientFromMap() {
        MapClientEntity mce = Dict.clientDelegate();
        assertThat(mce.getClientId(), nullValue());
        assertThat(mce.isEnabled(), nullValue());
        assertThat(mce.getAttribute("logo"), nullValue());
        assertThat(mce.getAttributes().keySet(), is(empty()));

        Dict.asDict(mce).put(Dict.CLIENT_FIELD_NAME, "name");
        Dict.asDict(mce).put(Dict.CLIENT_FIELD_ENABLED, false);
        Dict.asDict(mce).put(Dict.CLIENT_FIELD_LOGO, "thisShouldBeBase64Logo");
        Dict.asDict(mce).put("nonexistent", "nonexistent");

        assertThat(mce.getId(), is("name"));
        assertThat(mce.getClientId(), is("name"));
        assertThat(mce.isEnabled(), is(false));
        assertThat(mce.getAttribute("logo"), hasItems("thisShouldBeBase64Logo"));
        assertThat(mce.getAttributes().keySet(), hasItems("logo"));
    }

    @Test
    public void testDictClientFromEntity() {
        MapClientEntity mce = Dict.clientDelegate();
        
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_NAME), nullValue());
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_ENABLED), nullValue());
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), nullValue());

        mce.setClientId("name");
        mce.setEnabled(false);
        mce.setAttribute("logo", Arrays.asList("thisShouldBeBase64Logo"));
        mce.setAttribute("blah", Arrays.asList("thisShouldBeBase64Logofdas"));
        
        assertThat(mce.getAttributes().keySet(), hasItems("logo"));
        
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_NAME), is("name"));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_ENABLED), is(false));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), is("thisShouldBeBase64Logo"));
        
        mce.setAttribute("logo", Arrays.asList("thisShouldBeAnotherBase64Logo"));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), is("thisShouldBeAnotherBase64Logo"));

        mce.removeAttribute("logo");
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), nullValue());
    }
}
