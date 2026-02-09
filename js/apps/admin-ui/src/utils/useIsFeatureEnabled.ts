import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAccess } from "../context/access/Access";

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
  JWTAuthorizationGrant = "JWT_AUTHORIZATION_GRANT",
  Passkeys = "PASSKEYS",
  ClientAuthFederated = "CLIENT_AUTH_FEDERATED",
  Workflows = "WORKFLOWS",
}

export default function useIsFeatureEnabled() {
  const { features } = useServerInfo();
  const { hasAccess } = useAccess();

  const hasFeatureAccess = (feature: Feature) => {
    switch (feature) {
      case Feature.Organizations:
        return hasAccess("manage-realm");
      default:
        return true;
    }
  };

  return function isFeatureEnabled(feature: Feature) {
    if (!features) {
      return false;
    }
    return features
      .filter((f) => f.enabled && hasFeatureAccess(f.name as Feature))
      .map((f) => f.name)
      .includes(feature);
  };
}
