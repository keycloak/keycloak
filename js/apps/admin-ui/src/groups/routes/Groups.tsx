import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type GroupsParams = { realm: string; id?: string };

const GroupsSection = lazy(() => import("../GroupsSection"));

export const GroupsRoute: RouteDef = {
  path: "/:realm/groups/*",
  element: <GroupsSection />,
  handle: {
    access: "query-groups",
  },
};

export const GroupsWithIdRoute: RouteDef = {
  ...GroupsRoute,
  path: "/:realm/groups/:id",
};

export const toGroups = (params: GroupsParams): Partial<Path> => {
  const path = params.id ? GroupsWithIdRoute.path : GroupsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
