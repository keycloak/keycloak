import type { RouteDef } from "../route-config";
import {
  DashboardRoute,
  DashboardRouteWithRealm,
  DashboardRouteWithTab,
} from "./routes/Dashboard";

const routes: RouteDef[] = [
  DashboardRoute,
  DashboardRouteWithRealm,
  DashboardRouteWithTab,
];

export default routes;
