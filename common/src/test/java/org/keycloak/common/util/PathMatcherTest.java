package org.keycloak.common.util;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

public class PathMatcherTest {

    @Test
    public void keycloak15833Test() {
        TestingPathMatcher matcher = new TestingPathMatcher();

        Assert.assertEquals("/api/v1/1/campaigns/*/excelFiles", matcher.customBuildUriFromTemplate("/api/v1/{clientId}/campaigns/*/excelFiles", "/api/v1/1/contentConnectorConfigs/29/contentConnectorContents", false));
    }
    
    private static final class TestingPathMatcher extends PathMatcher<Object> {

        @Override
        protected String getPath(Object entry) {
            return null;
        }

        @Override
        protected Collection<Object> getPaths() {
            return null;
        }

        // Make buildUriFromTemplate accessible from test
        public String customBuildUriFromTemplate(String template, String targetUri, boolean onlyFirstParam) {
            return buildUriFromTemplate(template, targetUri, onlyFirstParam);
        }
    }
}
