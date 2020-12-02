import React from "react";
import ReactDom from "react-dom";
import i18n from "./i18n";

import init from "./context/auth/keycloak";
import { KeycloakAdminConsole } from "./KeycloakAdminConsole";

console.info("supported languages", ...i18n.languages);

init().then((adminClient) => {
  ReactDom.render(
    <KeycloakAdminConsole adminClient={adminClient} />,
    document.getElementById("app")
  );
});
