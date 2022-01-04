import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type ResourceDetailsParams = {
  realm: string;
  id: string;
  resourceId?: string;
};

export const ResourceDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/resource/:resourceId?",
  component: lazy(() => import("../authorization/ResourceDetails")),
  breadcrumb: (t) => t("clients:createResource"),
  access: "manage-clients",
};

export const toResourceDetails = (
  params: ResourceDetailsParams
): LocationDescriptorObject => ({
  pathname: generatePath(ResourceDetailsRoute.path, params),
});
