import React from "react";
import ReactDom from "react-dom";
import i18n from "./i18n";

import init from "./context/auth/keycloak";
import { AdminClient } from "./context/auth/AdminClient";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import { App } from "./App";

console.info("supported languages", ...i18n.languages);

init().then((adminClient) => {
  ReactDom.render(
    <AdminClient.Provider value={adminClient}>
      <WhoAmIContextProvider>
        <RealmContextProvider>
          <App />
        </RealmContextProvider>
      </WhoAmIContextProvider>
    </AdminClient.Provider>,
    document.getElementById("app")
  );
});
