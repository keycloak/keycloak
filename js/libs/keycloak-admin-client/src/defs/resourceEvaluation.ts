import type ResourceRepresentation from "./resourceRepresentation.js";

export default interface ResourceEvaluation {
  roleIds?: string[];
  clientId: string;
  userId: string;
  resources?: ResourceRepresentation[];
  entitlements: boolean;
  context: {
    attributes: {
      [key: string]: string;
    };
  };
}
