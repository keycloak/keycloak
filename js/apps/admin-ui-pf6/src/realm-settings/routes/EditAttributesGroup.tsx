import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("editGroupText"),
  },
};

export const toEditAttributesGroup = (
  params: EditAttributesGroupParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(EditAttributesGroupRoute.path, params),
});
