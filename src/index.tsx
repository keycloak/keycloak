import React from "react";
import ReactDom from "react-dom";
import i18n from "./i18n";

import { App } from "./App";
import init from "./auth/keycloak";
import { KeycloakContext } from "./auth/KeycloakContext";
import { KeycloakService } from "./auth/keycloak.service";
import { HttpClientContext } from "./http-service/HttpClientContext";
import { HttpClient } from "./http-service/http-client";

console.info("supported languages", ...i18n.languages);
init().then((keycloak) => {
  const keycloakService = new KeycloakService(keycloak);
  ReactDom.render(
    <KeycloakContext.Provider value={keycloakService}>
      <HttpClientContext.Provider value={new HttpClient(keycloakService)}>
        <App />
      </HttpClientContext.Provider>
    </KeycloakContext.Provider>,
    document.getElementById("app")
  );
});
