import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UsersSection } from "../UsersSection";

export type UsersParams = { realm: string };

export const UsersRoute: RouteDef = {
  path: "/:realm/users",
  component: UsersSection,
  breadcrumb: (t) => t("users:title"),
  access: "query-users",
};

export const toUsers = (params: UsersParams): LocationDescriptorObject => ({
  pathname: generatePath(UsersRoute.path, params),
});
