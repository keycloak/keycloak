import type { RouteDef } from "../route-config";
import { EventsRoute, EventsRouteWithTab } from "./routes/Events";

const routes: RouteDef[] = [EventsRoute, EventsRouteWithTab];

export default routes;
