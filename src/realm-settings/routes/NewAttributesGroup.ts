import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewAttributesGroupParams = {
  realm: string;
};

export const NewAttributesGroupRoute: RouteDef = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/new",
  component: lazy(() => import("../user-profile/AttributesGroupDetails")),
  breadcrumb: (t) => t("attributes-group:createGroupText"),
  access: "view-realm",
};

export const toNewAttributesGroup = (
  params: NewAttributesGroupParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewAttributesGroupRoute.path, params),
});
