import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import ReactDOM from "react-dom";
import { createBrowserRouter, RouterProvider } from "react-router-dom";

import { i18n } from "./i18n";
import { routes } from "./routes";

await i18n.init();

const router = createBrowserRouter(routes);

ReactDOM.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
  document.getElementById("app")
);
