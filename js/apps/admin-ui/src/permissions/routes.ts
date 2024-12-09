import type { AppRouteObject } from "../routes";
import { PermissionsRoute } from "./routes/Permissions";
import { PermissionsTabsRoute } from "./routes/PermissionsTabs";

const routes: AppRouteObject[] = [PermissionsRoute, PermissionsTabsRoute];

export default routes;
