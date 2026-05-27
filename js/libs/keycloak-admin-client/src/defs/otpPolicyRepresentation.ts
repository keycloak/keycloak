/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_otppolicyrepresentation
 */
export default interface OtpPolicyRepresentation {
  type: string;
  algorithm: string;
  digits: number;
  lookAheadWindow: number;
  period?: number;
  initialCounter?: number;
  codeReusable: boolean;
}
