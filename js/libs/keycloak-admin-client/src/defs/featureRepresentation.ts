export default interface FeatureRepresentation {
  name: string;
  label: string;
  type: FeatureType;
  enabled: boolean;
  dependencies: Set<string>;
}

export enum FeatureType {
  DEFAULT = "DEFAULT",
  DISABLED_BY_DEFAULT = "DISABLED_BY_DEFAULT",
  PREVIEW = "PREVIEW",
  PREVIEW_DISABLED_BY_DEFAULT = "PREVIEW_DISABLED_BY_DEFAULT",
  EXPERIMENTAL = "EXPERIMENTAL",
  DEPRECATED = "DEPRECATED",
}
