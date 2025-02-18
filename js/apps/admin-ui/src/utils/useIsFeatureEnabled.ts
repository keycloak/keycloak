import { useServerInfo } from "../context/server-info/ServerInfoProvider";

export enum Feature {
  AdminFineGrainedAuthz = "ADMIN_FINE_GRAINED_AUTHZ",
  AdminFineGrainedAuthzV2 = "ADMIN_FINE_GRAINED_AUTHZ_V2",
  ClientPolicies = "CLIENT_POLICIES",
  Kerberos = "KERBEROS",
  DynamicScopes = "DYNAMIC_SCOPES",
  DPoP = "DPOP",
  DeviceFlow = "DEVICE_FLOW",
  TransientUsers = "TRANSIENT_USERS",
  ClientTypes = "CLIENT_TYPES",
  DeclarativeUI = "DECLARATIVE_UI",
  Organizations = "ORGANIZATION",
  OpenId4VCI = "OID4VC_VCI",
  QuickTheme = "QUICK_THEME",
  StandardTokenExchangeV2 = "TOKEN_EXCHANGE_STANDARD_V2",
}

export default function useIsFeatureEnabled() {
  const { features } = useServerInfo();

  return function isFeatureEnabled(feature: Feature) {
    if (!features) {
      return false;
    }
    return features
      .filter((f) => f.enabled)
      .map((f) => f.name)
      .includes(feature);
  };
}
