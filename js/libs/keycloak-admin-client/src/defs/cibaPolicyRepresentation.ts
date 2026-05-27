/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_cibapolicyrepresentation
 */
export default interface CibaPolicyRepresentation {
  backchannelTokenDeliveryMode: string;
  expiresIn: number;
  poolingInterval: number;
  authRequestedUserHint: string;
}
