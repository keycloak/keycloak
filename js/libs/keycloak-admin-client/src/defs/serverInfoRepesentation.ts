import type ComponentTypeRepresentation from "./componentTypeRepresentation.js";
import type { ConfigPropertyRepresentation } from "./configPropertyRepresentation.js";
import FeatureRepresentation from "./featureRepresentation.js";
import type PasswordPolicyTypeRepresentation from "./passwordPolicyTypeRepresentation.js";
import type ProfileInfoRepresentation from "./profileInfoRepresentation.js";
import type ProtocolMapperRepresentation from "./protocolMapperRepresentation.js";
import type SystemInfoRepresentation from "./systemInfoRepersantation.js";

/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_serverinforepresentation
 */
export interface ServerInfoRepresentation {
  systemInfo?: SystemInfoRepresentation;
  memoryInfo?: MemoryInfoRepresentation;
  profileInfo?: ProfileInfoRepresentation;
  features?: FeatureRepresentation[];
  cryptoInfo?: CryptoInfoRepresentation;
  themes?: { [index: string]: ThemeInfoRepresentation[] };
  socialProviders?: { [index: string]: string }[];
  identityProviders?: { [index: string]: string }[];
  clientImporters?: { [index: string]: string }[];
  providers?: { [index: string]: SpiInfoRepresentation };
  protocolMapperTypes?: { [index: string]: ProtocolMapperTypeRepresentation[] };
  builtinProtocolMappers?: { [index: string]: ProtocolMapperRepresentation[] };
  clientInstallations?: { [index: string]: ClientInstallationRepresentation[] };
  componentTypes?: { [index: string]: ComponentTypeRepresentation[] };
  passwordPolicies?: PasswordPolicyTypeRepresentation[];
  enums?: { [index: string]: string[] };
}

export interface ThemeInfoRepresentation {
  name: string;
  locales?: string[];
}

export interface SpiInfoRepresentation {
  internal: boolean;
  providers: { [index: string]: ProviderRepresentation };
}

export interface ProviderRepresentation {
  order: number;
  operationalInfo?: Record<string, string>;
}

export interface ClientInstallationRepresentation {
  id: string;
  protocol: string;
  downloadOnly: boolean;
  displayType: string;
  helpText: string;
  filename: string;
  mediaType: string;
}

export interface MemoryInfoRepresentation {
  total: number;
  totalFormated: string;
  used: number;
  usedFormated: string;
  free: number;
  freePercentage: number;
  freeFormated: string;
}

export interface ProtocolMapperTypeRepresentation {
  id: string;
  name: string;
  category: string;
  helpText: string;
  priority: number;
  properties: ConfigPropertyRepresentation[];
}

export interface CryptoInfoRepresentation {
  cryptoProvider: string;
  supportedKeystoreTypes: string[];
  clientSignatureSymmetricAlgorithms: string[];
  clientSignatureAsymmetricAlgorithms: string[];
}
