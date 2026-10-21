import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type NewRoleParams = { realm: string; clientId: string };

const CreateClientRole = lazy(() => import("../roles/CreateClientRole"));

export const NewRoleRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/roles/new",
  element: <CreateClientRole />,
  handle: {
    access: "query-clients",
    breadcrumb: (t) => t("createRole"),
  },
};

export const toCreateRole = (params: NewRoleParams): Partial<Path> => ({
  pathname: generateEncodedPath(NewRoleRoute.path, params),
});
