import { lazy } from "react";
import { generatePath, type Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type GroupsParams = { realm: string; id?: string; lazy?: string };

const GroupsSection = lazy(() => import("../GroupsSection"));

export const GroupsRoute: AppRouteObject = {
  path: "/:realm/groups/*",
  element: <GroupsSection />,
  handle: {
    access: "query-groups",
  },
};

export const GroupsWithIdRoute: AppRouteObject = {
  ...GroupsRoute,
  path: "/:realm/groups/:id",
};

export const toGroups = (params: GroupsParams): Partial<Path> => {
  const path = params.id ? GroupsWithIdRoute.path : GroupsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
