export default interface FeatureRepresentation {
  name: string;
  label: string;
  type: FeatureType;
  enabled: boolean;
  dependencies: string[];
}

export enum FeatureType {
  Default = "DEFAULT",
  DisabledByDefault = "DISABLED_BY_DEFAULT",
  Preview = "PREVIEW",
  PreviewDisabledByDefault = "PREVIEW_DISABLED_BY_DEFAULT",
  Experimental = "EXPERIMENTAL",
  Deprecated = "DEPRECATED",
}
