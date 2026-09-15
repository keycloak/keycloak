import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ClientScopesParams = { realm: string };

const ClientScopesSection = lazy(() => import("../ClientScopesSection"));

export const ClientScopesRoute: AppRouteObject = {
  path: "/:realm/client-scopes",
  element: <ClientScopesSection />,
  handle: {
    access: "view-clients",
    breadcrumb: (t) => t("clientScopeList"),
  },
};

export const toClientScopes = (params: ClientScopesParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientScopesRoute.path, params),
});
