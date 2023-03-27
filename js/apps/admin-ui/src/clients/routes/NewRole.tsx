import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewRoleParams = { realm: string; clientId: string };

const CreateClientRole = lazy(() => import("../roles/CreateClientRole"));

export const NewRoleRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/roles/new",
  element: <CreateClientRole />,
  breadcrumb: (t) => t("roles:createRole"),
  handle: {
    access: "manage-clients",
  },
};

export const toCreateRole = (params: NewRoleParams): Partial<Path> => ({
  pathname: generatePath(NewRoleRoute.path, params),
});
