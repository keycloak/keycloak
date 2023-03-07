import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewRoleParams = { realm: string; clientId: string };

export const NewRoleRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/new",
  component: lazy(() => import("../roles/CreateClientRole")),
  breadcrumb: (t) => t("roles:createRole"),
  access: "manage-clients",
};

export const toCreateRole = (params: NewRoleParams): Partial<Path> => ({
  pathname: generatePath(NewRoleRoute.path, params),
});
