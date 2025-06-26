/** TIDECLOAK IMPLEMENTATION */
import type { AppRouteObject } from "../../routes";
import { TideKeyRoute, TideKeyRouteWithTab } from "./routes/TideKeys";


const routes: AppRouteObject[] = [
  TideKeyRoute,
  TideKeyRouteWithTab
];

export default routes;
