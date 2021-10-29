import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EventsTab = "userEvents" | "adminEvents";

export type EventsParams = {
  realm: string;
  tab?: EventsTab;
};

export const EventsRoute: RouteDef = {
  path: "/:realm/events/:tab?",
  component: lazy(() => import("../EventsSection")),
  breadcrumb: (t) => t("events:title"),
  access: "view-events",
};

export const toEvents = (params: EventsParams): LocationDescriptorObject => ({
  pathname: generatePath(EventsRoute.path, params),
});
