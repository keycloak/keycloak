import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EditAttributesGroupParams = {
  realm: string;
  name: string;
};

const AttributesGroupDetails = lazy(
  () => import("../user-profile/AttributesGroupDetails")
);

export const EditAttributesGroupRoute: RouteDef = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/edit/:name",
  element: <AttributesGroupDetails />,
  breadcrumb: (t) => t("realm-settings:editGroupText"),
  access: "view-realm",
};

export const toEditAttributesGroup = (
  params: EditAttributesGroupParams
): Partial<Path> => ({
  pathname: generatePath(EditAttributesGroupRoute.path, params),
});
