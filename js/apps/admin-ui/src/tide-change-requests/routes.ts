/** TIDE IMPLEMENTATION */

import type { AppRouteObject } from "../routes";
import { ChangeRequestsRoute, ChangeRequestsRouteWithTab } from "./routes/ChangeRequests";


const routes: AppRouteObject[] = [
  ChangeRequestsRoute,
  ChangeRequestsRouteWithTab
];

export default routes;
