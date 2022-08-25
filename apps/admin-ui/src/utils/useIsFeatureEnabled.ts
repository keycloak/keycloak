import { useServerInfo } from "../context/server-info/ServerInfoProvider";

export enum Feature {
  DeclarativeUserProfile = "DECLARATIVE_USER_PROFILE",
}

export default function useIsFeatureEnabled() {
  const { profileInfo } = useServerInfo();

  const experimentalFeatures = profileInfo?.experimentalFeatures ?? [];
  const previewFeatures = profileInfo?.previewFeatures ?? [];
  const disabledFilters = profileInfo?.disabledFeatures ?? [];
  const allFeatures = [...experimentalFeatures, ...previewFeatures];
  const enabledFeatures = allFeatures.filter(
    (feature) => !disabledFilters.includes(feature)
  );

  return function isFeatureEnabled(feature: Feature) {
    return enabledFeatures.includes(feature);
  };
}
