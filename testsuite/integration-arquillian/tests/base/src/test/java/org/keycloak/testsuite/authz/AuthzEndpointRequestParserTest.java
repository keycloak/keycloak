package org.keycloak.testsuite.authz;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RealmBuilder;

public class AuthzEndpointRequestParserTest extends AbstractTestRealmKeycloakTest {

  @Override
  public void configureTestRealm(RealmRepresentation testRealm) {
    
    testRealm.setAttributes(new HashMap<>()); // no realm specific attributes yet
    
    RealmBuilder.edit(testRealm);
    
  }

  private void updateTestRealm(Map<String, String> newAttributes) {
      RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
      testRealm.setAttributes(newAttributes);
      adminClient.realm("test").update(testRealm);
  }
  
  @Test
  public void test_authentication_backwards_compatible() {
    
    // no realm specific attribute set - test backwards compatibility
    updateTestRealm(Collections.emptyMap());
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      oauth.addCustomParameter("paramkey1_too_long",          RandomStringUtils.random(2000 + 1));
      oauth.addCustomParameter("paramkey2",                   "paramvalue2");
      oauth.addCustomParameter("paramkey3",                   "paramvalue3");
      oauth.addCustomParameter("paramkey4",                   "paramvalue4");
      oauth.addCustomParameter("paramkey5",                   "paramvalue5");
      oauth.addCustomParameter("paramkey6_too_many",          "paramvalue6");
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_size_exceeds_failfast() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      oauth.addCustomParameter("param_too_long", RandomStringUtils.random(42 + 1));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response, Matchers.body(containsString("Back to Application")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_size_accepted() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      oauth.addCustomParameter("param_accepted", RandomStringUtils.random(42));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_size_exceeds_ignore() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "false",
            "additionalReqParamsMaxSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // well known params are ignored anyway
      oauth.addCustomParameter(OIDCLoginProtocol.NONCE_PARAM, RandomStringUtils.random(100));
      oauth.addCustomParameter("param_too_long_silently_ignored", RandomStringUtils.random(42 + 1));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_maxnumber_exceeds_failfast() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxNumber", "2"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // well known params are ignored
      oauth.addCustomParameter(OIDCLoginProtocol.NONCE_PARAM, RandomStringUtils.random(42));
      oauth.addCustomParameter("paramkey1", "paramvalue1");
      oauth.addCustomParameter("paramkey2", "paramvalue2");
      oauth.addCustomParameter("paramkey3", "paramvalue3");
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response, Matchers.body(containsString("Back to Application")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_maxnumber_accepted() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxNumber", "2"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // well known params are ignored
      oauth.addCustomParameter(OIDCLoginProtocol.NONCE_PARAM, RandomStringUtils.random(42));
      oauth.addCustomParameter("paramkey1", "paramvalue1");
      oauth.addCustomParameter("paramkey2", "paramvalue2");
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_maxnumber_exceeds_ignore() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "false",
            "additionalReqParamsMaxNumber", "2"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // well known params are ignored anyway
      oauth.addCustomParameter(OIDCLoginProtocol.NONCE_PARAM, RandomStringUtils.random(42));
      oauth.addCustomParameter("paramkey1", "paramvalue1");
      oauth.addCustomParameter("paramkey2", "paramvalue2");
      oauth.addCustomParameter("paramkey3", "paramvalue3");
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }

  @Test
  public void test_authentication_maxoverallsize_exceeds_failfast() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxOverallSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // 21 + 21 + 1 = 43
      oauth.addCustomParameter("paramkey1", RandomStringUtils.random(21));
      oauth.addCustomParameter("paramkey2", RandomStringUtils.random(21));
      oauth.addCustomParameter("paramkey3", RandomStringUtils.random(1));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response, Matchers.body(containsString("Back to Application")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_maxoverallsize_accepted() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxOverallSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // 21 + 21 = 42
      oauth.addCustomParameter("paramkey1", RandomStringUtils.random(21));
      oauth.addCustomParameter("paramkey2", RandomStringUtils.random(21));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_maxoverallsize_exceeds_ignore() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "false",
            "additionalReqParamsMaxOverallSize", "42"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      // 21 + 21 + 1 = 43
      oauth.addCustomParameter("paramkey1", RandomStringUtils.random(21));
      oauth.addCustomParameter("paramkey2", RandomStringUtils.random(21));
      oauth.addCustomParameter("paramkey3", RandomStringUtils.random(1));
      
      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
  @Test
  public void test_authentication_knownparameters_dont_count() {
    
    updateTestRealm(
        Map.of(
            "additionalReqParamsFailFast", "true",
            "additionalReqParamsMaxOverallSize", "42",
            "additionalReqParamsMaxNumber", "2"
        )
    );
    
    try (Client client = AdminClientUtil.createResteasyClient()) {
      
      /*
       * Well known parameter will neither be counted towards additionalReqParamsMaxSize nor
       * additionalReqParamsMaxOverallSize.
       */
      oauth.addCustomParameter(OIDCLoginProtocol.NONCE_PARAM, RandomStringUtils.random(100));
      oauth.addCustomParameter("paramkey1", RandomStringUtils.random(21));
      oauth.addCustomParameter(OIDCLoginProtocol.CODE_PARAM, "");
      oauth.addCustomParameter("paramkey2", RandomStringUtils.random(21));
      oauth.addCustomParameter(OIDCLoginProtocol.MAX_AGE_PARAM, "42");

      try (Response response = client.target(oauth.getLoginFormUrl()).request().get()) {
        
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));
        
      }
      
    }
    
  }
  
}
