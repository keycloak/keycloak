/** TIDECLOAK IMPLEMENTATION */

import type { AppRouteObject } from "../routes";
import {
  TidePoliciesRoute,
  TidePoliciesRouteWithTab,
} from "./routes/TidePolicies";

const routes: AppRouteObject[] = [
  TidePoliciesRoute,
  TidePoliciesRouteWithTab,
];

export default routes;
