import type { ConfigPropertyRepresentation } from "./configPropertyRepresentation.js";

export default interface EventHookProviderRepresentation {
  id: string;
  supportsBatch?: boolean;
  supportsRetry?: boolean;
  supportsAggregation?: boolean;
  configMetadata?: ConfigPropertyRepresentation[];
}
