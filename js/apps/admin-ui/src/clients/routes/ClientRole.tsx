import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

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

export const ClientRoleRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/roles/:id/:tab" as const,
  element: <RealmRoleTabs />,
  breadcrumb: (t) => t("roleDetails"),
  handle: {
    access: "query-clients",
  },
} satisfies AppRouteObject;

export const toClientRole = (params: ClientRoleParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientRoleRoute.path, params),
});
