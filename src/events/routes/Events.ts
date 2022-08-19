import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type EventsTab = "user-events" | "admin-events";

export type EventsParams = {
  realm: string;
  tab?: EventsTab;
};

export const EventsRoute: RouteDef = {
  path: "/:realm/events/:tab?",
  component: lazy(() => import("../EventsSection")),
  breadcrumb: (t) => t("events:title"),
  access: "view-events",
  legacy: true,
};

export const toEvents = (params: EventsParams): Partial<Path> => ({
  pathname: generatePath(EventsRoute.path, params),
});
