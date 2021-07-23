import type { RouteDef } from "../route-config";
import { AesGeneratedSettingsRoute } from "./routes/AesGeneratedSettings";
import { EcdsaGeneratedSettingsRoute } from "./routes/EcdsaGeneratedSettings";
import { HmacGeneratedSettingsRoute } from "./routes/HmacGeneratedSettings";
import { JavaKeystoreSettingsRoute } from "./routes/JavaKeystoreSettings";
import { RealmSettingsRoute } from "./routes/RealmSettings";
import { RsaGeneratedSettingsRoute } from "./routes/RsaGeneratedSettings";
import { RsaSettingsRoute } from "./routes/RsaSettings";

const routes: RouteDef[] = [
  RealmSettingsRoute,
  AesGeneratedSettingsRoute,
  EcdsaGeneratedSettingsRoute,
  HmacGeneratedSettingsRoute,
  JavaKeystoreSettingsRoute,
  RsaGeneratedSettingsRoute,
  RsaSettingsRoute,
];

export default routes;
