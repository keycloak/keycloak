import { KeycloakAdminClient } from "./client.js";
import { RequiredActionAlias } from "./defs/requiredActionProviderRepresentation.js";

export const requiredAction = RequiredActionAlias;
export default KeycloakAdminClient;
export { NetworkError, fetchWithError } from "./utils/fetchWithError.js";
export type { NetworkErrorOptions } from "./utils/fetchWithError.js";
