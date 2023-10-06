import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";
import type { AppRouteObject } from "../../routes";

export type EditAttributesGroupParams = {
  realm: string;
  name: string;
};

const AttributesGroupDetails = lazy(
  () => import("../user-profile/AttributesGroupDetails"),
);

export const EditAttributesGroupRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/edit/:name",
  element: <AttributesGroupDetails />,
  breadcrumb: (t) => t("editGroupText"),
  handle: {
    access: "view-realm",
  },
};

export const toEditAttributesGroup = (
  params: EditAttributesGroupParams,
): Partial<Path> => ({
  pathname: generateUnencodedPath(EditAttributesGroupRoute.path, params),
});
