import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EventsTab = "user-events" | "admin-events";

export type EventsParams = {
  realm: string;
  tab?: EventsTab;
};

export const EventsRoute: RouteDef = {
  path: "/:realm/events",
  component: lazy(() => import("../EventsSection")),
  breadcrumb: (t) => t("events:title"),
  access: "view-events",
};

export const EventsRouteWithTab: RouteDef = {
  ...EventsRoute,
  path: "/:realm/events/:tab",
};

export const toEvents = (params: EventsParams): Partial<Path> => {
  const path = params.tab ? EventsRouteWithTab.path : EventsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
