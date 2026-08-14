import type { AppRouteObject } from "../routes";
import {
  EventsRoute,
  EventsRouteWithSubTab,
  EventsRouteWithTab,
} from "./routes/Events";

const routes: AppRouteObject[] = [
  EventsRoute,
  EventsRouteWithTab,
  EventsRouteWithSubTab,
];

export default routes;
