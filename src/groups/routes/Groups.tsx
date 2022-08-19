import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type GroupsParams = { realm: string; id?: string };

export const GroupsRoute: RouteDef = {
  path: "/:realm/groups/:id?",
  component: lazy(() => import("../GroupsSection")),
  access: "query-groups",
  matchOptions: {
    exact: false,
  },
  legacy: true,
};

export const toGroups = (params: GroupsParams): Partial<Path> => ({
  pathname: generatePath(GroupsRoute.path, params),
});
