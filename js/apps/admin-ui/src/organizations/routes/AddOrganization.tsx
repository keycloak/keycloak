import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddOrganizationParams = { realm: string };

const NewOrganization = lazy(() => import("../NewOrganization"));

export const AddOrganizationRoute: AppRouteObject = {
  path: "/:realm/organizations/new",
  element: <NewOrganization />,
  breadcrumb: (t) => t("createOrganization"),
  handle: {
    access: "manage-users",
  },
};

export const toAddOrganization = (
  params: AddOrganizationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(AddOrganizationRoute.path, params),
});
