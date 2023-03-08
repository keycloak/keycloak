import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewResourceParams = { realm: string; id: string };

export const NewResourceRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/resource/new",
  component: lazy(() => import("../authorization/ResourceDetails")),
  breadcrumb: (t) => t("clients:createResource"),
  access: "view-clients",
};

export const toCreateResource = (params: NewResourceParams): Partial<Path> => ({
  pathname: generatePath(NewResourceRoute.path, params),
});
