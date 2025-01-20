import type { AppRouteObject } from "../routes";
import { NewPermissionConfigurationRoute } from "./routes/NewPermissionConfiguration";
import { NewPermissionPolicyRoute } from "./routes/NewPermissionPolicy";
import { PermissionConfigurationDetailRoute } from "./routes/PermissionConfigurationDetails";
import { PermissionPolicyDetailsRoute } from "./routes/PermissionPolicyDetails";
import { PermissionsConfigurationRoute } from "./routes/PermissionsConfiguration";
import { PermissionsConfigurationTabsRoute } from "./routes/PermissionsConfigurationTabs";

const routes: AppRouteObject[] = [
  NewPermissionConfigurationRoute,
  PermissionConfigurationDetailRoute,
  PermissionsConfigurationRoute,
  PermissionsConfigurationTabsRoute,
  NewPermissionPolicyRoute,
  PermissionPolicyDetailsRoute,
];

export default routes;
