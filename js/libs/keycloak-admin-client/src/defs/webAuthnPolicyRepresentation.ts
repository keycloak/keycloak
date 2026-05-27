/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_webauthnpolicyrepresentation
 */
export default interface WebAuthnPolicyRepresentation {
  rpEntityName: string;
  signatureAlgorithms: string[];
  rpId: string;
  attestationConveyancePreference: string;
  authenticatorAttachment: string;
  requireResidentKey: string;
  userVerificationRequirement: string;
  createTimeout: number;
  avoidSameAuthenticatorRegister: boolean;
  acceptableAaguids: string[];
  extraOrigins: string[];
  passkeysEnabled: boolean;
  mediation?: string;
}
