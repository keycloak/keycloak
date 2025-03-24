import type { AppRouteObject } from "../routes";
import { NewPermissionConfigurationRoute } from "./routes/NewPermissionConfiguration";
import { NewPermissionPolicyRoute } from "./routes/NewPermissionPolicy";
import { PermissionConfigurationDetailRoute } from "./routes/PermissionConfigurationDetails";
import { PermissionPolicyDetailsRoute } from "./routes/PermissionPolicyDetails";
import { PermissionsConfigurationRoute } from "./routes/PermissionsConfiguration";
import { PermissionsConfigurationTabsRoute } from "./routes/PermissionsConfigurationTabs";
import { PermissionsPoliciesRoute } from "./routes/PermissionsPolicies";

const routes: AppRouteObject[] = [
  NewPermissionConfigurationRoute,
  PermissionConfigurationDetailRoute,
  PermissionsConfigurationRoute,
  PermissionsConfigurationTabsRoute,
  PermissionsPoliciesRoute,
  NewPermissionPolicyRoute,
  PermissionPolicyDetailsRoute,
];

export default routes;
