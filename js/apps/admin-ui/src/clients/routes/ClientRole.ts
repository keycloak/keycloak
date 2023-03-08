import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientRoleTab =
  | "details"
  | "attributes"
  | "users-in-role"
  | "associated-roles";

export type ClientRoleParams = {
  realm: string;
  clientId: string;
  id: string;
  tab: ClientRoleTab;
};

export const ClientRoleRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/:id/:tab" as const,
  component: lazy(() => import("../../realm-roles/RealmRoleTabs")),
  breadcrumb: (t) => t("roles:roleDetails"),
  access: "view-realm",
} satisfies RouteDef;

export const toClientRole = (params: ClientRoleParams): Partial<Path> => ({
  pathname: generatePath(ClientRoleRoute.path, params),
});
