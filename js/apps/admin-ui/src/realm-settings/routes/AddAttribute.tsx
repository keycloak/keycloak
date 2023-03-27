import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type AddAttributeParams = {
  realm: string;
};

const NewAttributeSettings = lazy(() => import("../NewAttributeSettings"));

export const AddAttributeRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributes/add-attribute",
  element: <NewAttributeSettings />,
  breadcrumb: (t) => t("realm-settings:createAttribute"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddAttribute = (params: AddAttributeParams): Partial<Path> => ({
  pathname: generatePath(AddAttributeRoute.path, params),
});
