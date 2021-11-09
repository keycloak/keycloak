import type { RouteDef } from "../route-config";
import { AesGeneratedSettingsRoute } from "./routes/AesGeneratedSettings";
import { EcdsaGeneratedSettingsRoute } from "./routes/EcdsaGeneratedSettings";
import { HmacGeneratedSettingsRoute } from "./routes/HmacGeneratedSettings";
import { JavaKeystoreSettingsRoute } from "./routes/JavaKeystoreSettings";
import { RealmSettingsRoute } from "./routes/RealmSettings";
import { RsaGeneratedSettingsRoute } from "./routes/RsaGeneratedSettings";
import { RsaSettingsRoute } from "./routes/RsaSettings";
import { ClientPoliciesRoute } from "./routes/ClientPolicies";
import { AddClientProfileRoute } from "./routes/AddClientProfile";
import { ClientProfileRoute } from "./routes/ClientProfile";
import { AddExecutorRoute } from "./routes/AddExecutor";
import { AddClientPolicyRoute } from "./routes/AddClientPolicy";
import { EditClientPolicyRoute } from "./routes/EditClientPolicy";
import { NewClientPolicyConditionRoute } from "./routes/AddCondition";
import { EditClientPolicyConditionRoute } from "./routes/EditCondition";

const routes: RouteDef[] = [
  RealmSettingsRoute,
  AesGeneratedSettingsRoute,
  EcdsaGeneratedSettingsRoute,
  HmacGeneratedSettingsRoute,
  JavaKeystoreSettingsRoute,
  RsaGeneratedSettingsRoute,
  RsaSettingsRoute,
  ClientPoliciesRoute,
  AddClientProfileRoute,
  AddExecutorRoute,
  ClientProfileRoute,
  AddClientPolicyRoute,
  EditClientPolicyRoute,
  NewClientPolicyConditionRoute,
  EditClientPolicyConditionRoute,
];

export default routes;
