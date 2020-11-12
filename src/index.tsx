import React from "react";
import ReactDom from "react-dom";
import i18n from "./i18n";

import { AdminClient } from "./context/auth/AdminClient";
import init from "./context/auth/keycloak";
import { App } from "./App";
import { RealmContextProvider } from "./context/realm-context/RealmContext";

console.info("supported languages", ...i18n.languages);

init().then((adminClient) => {
  ReactDom.render(
    <RealmContextProvider>
      <AdminClient.Provider value={adminClient}>
        <App />
      </AdminClient.Provider>
    </RealmContextProvider>,
    document.getElementById("app")
  );
});
