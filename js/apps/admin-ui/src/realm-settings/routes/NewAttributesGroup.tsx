import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewAttributesGroupParams = {
  realm: string;
};

const AttributesGroupDetails = lazy(
  () => import("../user-profile/AttributesGroupDetails")
);

export const NewAttributesGroupRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/new",
  element: <AttributesGroupDetails />,
  breadcrumb: (t) => t("realm-settings:createGroupText"),
  handle: {
    access: "view-realm",
  },
};

export const toNewAttributesGroup = (
  params: NewAttributesGroupParams
): Partial<Path> => ({
  pathname: generatePath(NewAttributesGroupRoute.path, params),
});
