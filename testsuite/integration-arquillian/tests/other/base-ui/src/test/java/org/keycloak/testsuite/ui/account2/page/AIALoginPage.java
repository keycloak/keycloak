package org.keycloak.testsuite.ui.account2.page;

import java.net.URI;
import java.util.stream.Stream;

import org.keycloak.testsuite.auth.page.login.OIDCLogin;

public class AIALoginPage extends OIDCLogin {

  @Override
  public boolean isCurrent() {
    URI uri = URI.create(driver.getCurrentUrl());
    return super.isCurrent() && uri.getQuery().contains("kc_action");
  }

  public String getCurrentAction() {
    URI uri = URI.create(driver.getCurrentUrl());
    String[] queryFragments = uri.getQuery().split("&");
    return Stream.of(queryFragments).filter(fragment -> fragment.contains("kc_action")).map(fragment -> fragment.split("=")[1]).findFirst().get();
  }
}
