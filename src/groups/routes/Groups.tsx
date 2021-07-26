import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { GroupsSection } from "../GroupsSection";

export type GroupsParams = { realm: string };

export const GroupsRoute: RouteDef = {
  path: "/:realm/groups",
  component: GroupsSection,
  access: "query-groups",
  matchOptions: {
    exact: false,
  },
};

export const toGroups = (params: GroupsParams): LocationDescriptorObject => ({
  pathname: generatePath(GroupsRoute.path, params),
});
