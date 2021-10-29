import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewClientScopeParams = { realm: string };

export const NewClientScopeRoute: RouteDef = {
  path: "/:realm/client-scopes/new",
  component: lazy(() => import("../form/ClientScopeForm")),
  breadcrumb: (t) => t("client-scopes:createClientScope"),
  access: "manage-clients",
};

export const toNewClientScope = (
  params: NewClientScopeParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewClientScopeRoute.path, params),
});
