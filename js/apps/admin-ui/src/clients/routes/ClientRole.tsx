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

const RealmRoleTabs = lazy(() => import("../../realm-roles/RealmRoleTabs"));

export const ClientRoleRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/:id/:tab" as const,
  element: <RealmRoleTabs />,
  breadcrumb: (t) => t("roles:roleDetails"),
  handle: {
    access: "view-realm",
  },
} satisfies RouteDef;

export const toClientRole = (params: ClientRoleParams): Partial<Path> => ({
  pathname: generatePath(ClientRoleRoute.path, params),
});
