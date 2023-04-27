import { useServerInfo } from "../context/server-info/ServerInfoProvider";

export enum Feature {
  AdminFineGrainedAuthz = "ADMIN_FINE_GRAINED_AUTHZ",
  ClientPolicies = "CLIENT_POLICIES",
  DeclarativeUserProfile = "DECLARATIVE_USER_PROFILE",
  Kerberos = "KERBEROS",
  DynamicScopes = "DYNAMIC_SCOPES",
}

export default function useIsFeatureEnabled() {
  const { profileInfo } = useServerInfo();
  const disabledFilters = profileInfo?.disabledFeatures ?? [];

  return function isFeatureEnabled(feature: Feature) {
    return !disabledFilters.includes(feature);
  };
}
