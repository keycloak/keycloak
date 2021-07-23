import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EventsSection } from "../EventsSection";

export type EventsTab = "userEvents" | "adminEvents";

export type EventsParams = {
  realm: string;
  tab?: EventsTab;
};

export const EventsRoute: RouteDef = {
  path: "/:realm/events/:tab?",
  component: EventsSection,
  breadcrumb: (t) => t("events:title"),
  access: "view-events",
};

export const toEvents = (params: EventsParams): LocationDescriptorObject => ({
  pathname: generatePath(EventsRoute.path, params),
});
