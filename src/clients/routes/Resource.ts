import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ResourceDetailsParams = {
  realm: string;
  id: string;
  resourceId?: string;
};

export const ResourceDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/resource/:resourceId?",
  component: lazy(() => import("../authorization/ResourceDetails")),
  breadcrumb: (t) => t("clients:createResource"),
  access: "view-clients",
  legacy: true,
};

export const toResourceDetails = (
  params: ResourceDetailsParams
): Partial<Path> => ({
  pathname: generatePath(ResourceDetailsRoute.path, params),
});
