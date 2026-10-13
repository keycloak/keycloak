import type { AppRouteObject } from "../routes";
import { AddAttributeRoute } from "./routes/AddAttribute";
import { AddClientPolicyRoute } from "./routes/AddClientPolicy";
import { AddClientProfileRoute } from "./routes/AddClientProfile";
import { NewClientPolicyConditionRoute } from "./routes/AddCondition";
import { AddExecutorRoute } from "./routes/AddExecutor";
import { AttributeRoute } from "./routes/Attribute";
import { ClientPoliciesRoute } from "./routes/ClientPolicies";
import { ClientProfileRoute } from "./routes/ClientProfile";
import { EditAttributesGroupRoute } from "./routes/EditAttributesGroup";
import { EditClientPolicyRoute } from "./routes/EditClientPolicy";
import { EditClientPolicyConditionRoute } from "./routes/EditCondition";
import { ExecutorRoute } from "./routes/Executor";
import { KeyProviderFormRoute } from "./routes/KeyProvider";
import { KeysRoute } from "./routes/KeysTab";
import { NewAttributesGroupRoute } from "./routes/NewAttributesGroup";
import {
  RealmSettingsRoute,
  RealmSettingsRouteWithTab,
} from "./routes/RealmSettings";
import { ThemeTabRoute } from "./routes/ThemesTab";
import { UserProfileRoute } from "./routes/UserProfile";

const routes: AppRouteObject[] = [
  RealmSettingsRoute,
  RealmSettingsRouteWithTab,
  KeysRoute,
  KeyProviderFormRoute,
  ClientPoliciesRoute,
  AddClientProfileRoute,
  AddExecutorRoute,
  ClientProfileRoute,
  ExecutorRoute,
  AddClientPolicyRoute,
  EditClientPolicyRoute,
  NewClientPolicyConditionRoute,
  EditClientPolicyConditionRoute,
  UserProfileRoute,
  AddAttributeRoute,
  AttributeRoute,
  NewAttributesGroupRoute,
  EditAttributesGroupRoute,
  ThemeTabRoute,
];

export default routes;
