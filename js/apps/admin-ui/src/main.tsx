import "@patternfly/react-core/dist/styles/base.css";
import "@patternfly/patternfly/patternfly-addons.css";

import { StrictMode } from "react";
import { render } from "react-dom";
import { createHashRouter, RouterProvider } from "react-router-dom";

import { RootRoute } from "./routes";

import "./index.css";

const router = createHashRouter([RootRoute]);
const container = document.getElementById("app");

render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
  container
);
