import React from "react";
import ReactDom from "react-dom";
import i18n from "./i18n";

import { App } from "./App";
import init from "./context/auth/keycloak";
import { KeycloakContext } from "./context/auth/KeycloakContext";
import { KeycloakService } from "./context/auth/keycloak.service";
import { HttpClientContext } from "./context/http-service/HttpClientContext";
import { HttpClient } from "./context/http-service/http-client";

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
