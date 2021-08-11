import "./index.css";
import React, { StrictMode } from "react";
import ReactDOM from "react-dom";

import { App } from "./App";
import i18n from "./i18n";
import init from "./context/auth/keycloak";

console.info("supported languages", ...i18n.languages);

init().then((adminClient) => {
  ReactDOM.render(
    <StrictMode>
      <App adminClient={adminClient} />
    </StrictMode>,
    document.getElementById("app")
  );
});

// Hot Module Replacement (HMR) - Remove this snippet to remove HMR.
// Learn more: https://snowpack.dev/concepts/hot-module-replacement
if (import.meta.hot) {
  import.meta.hot.accept();
}
