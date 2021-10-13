import type { RouteDef } from "../route-config";
import { AesGeneratedSettingsRoute } from "./routes/AesGeneratedSettings";
import { EcdsaGeneratedSettingsRoute } from "./routes/EcdsaGeneratedSettings";
import { HmacGeneratedSettingsRoute } from "./routes/HmacGeneratedSettings";
import { JavaKeystoreSettingsRoute } from "./routes/JavaKeystoreSettings";
import { RealmSettingsRoute } from "./routes/RealmSettings";
import { RsaGeneratedSettingsRoute } from "./routes/RsaGeneratedSettings";
import { RsaSettingsRoute } from "./routes/RsaSettings";
import { ClientPoliciesRoute } from "./routes/ClientPolicies";
import { NewClientProfileRoute } from "./routes/NewClientProfile";
import { NewClientPolicyRoute } from "./routes/NewClientPolicy";
import { EditClientPolicyRoute } from "./routes/EditClientPolicy";

const routes: RouteDef[] = [
  RealmSettingsRoute,
  AesGeneratedSettingsRoute,
  EcdsaGeneratedSettingsRoute,
  HmacGeneratedSettingsRoute,
  JavaKeystoreSettingsRoute,
  RsaGeneratedSettingsRoute,
  RsaSettingsRoute,
  ClientPoliciesRoute,
  NewClientProfileRoute,
  NewClientPolicyRoute,
  EditClientPolicyRoute,
];

export default routes;
