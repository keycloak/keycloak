import type { AppRouteObject } from "../routes";
import { EventsRoute, EventsRouteWithTab } from "./routes/Events";

const routes: AppRouteObject[] = [EventsRoute, EventsRouteWithTab];

export default routes;
