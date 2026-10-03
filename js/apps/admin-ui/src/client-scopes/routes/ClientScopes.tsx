import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  ClientScopesRoutePath,
  type ClientScopesParams,
  toClientScopes,
} from "./ClientScopes.routes";

export type { ClientScopesParams };
export { toClientScopes };

const ClientScopesSection = lazy(() => import("../ClientScopesSection"));

export const ClientScopesRoute: AppRouteObject = {
  path: ClientScopesRoutePath,
  element: <ClientScopesSection />,
  handle: {
    access: "view-clients",
    breadcrumb: (t) => t("clientScopeList"),
  },
};
