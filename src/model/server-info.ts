export interface ServerInfoRepresentation {
  systemInfo: SystemInfoRepresentation;
  memoryInfo: MemoryInfoRepresentation;
  profileInfo: ProfileInfoRepresentation;
  themes: { [index: string]: ThemeInfoRepresentation[] };
  socialProviders: { [index: string]: string }[];
  identityProviders: { [index: string]: string }[];
  clientImporters: { [index: string]: string }[];
  providers: { [index: string]: SpiInfoRepresentation };
  protocolMapperTypes: { [index: string]: ProtocolMapperTypeRepresentation[] };
  builtinProtocolMappers: { [index: string]: ProtocolMapperRepresentation[] };
  clientInstallations: { [index: string]: ClientInstallationRepresentation[] };
  componentTypes: { [index: string]: ComponentTypeRepresentation[] };
  passwordPolicies: PasswordPolicyTypeRepresentation[];
  enums: { [index: string]: string[] };
}

export interface SystemInfoRepresentation {
  version: string;
  serverTime: string;
  uptime: string;
  uptimeMillis: number;
  javaVersion: string;
  javaVendor: string;
  javaVm: string;
  javaVmVersion: string;
  javaRuntime: string;
  javaHome: string;
  osName: string;
  osArchitecture: string;
  osVersion: string;
  fileEncoding: string;
  userName: string;
  userDir: string;
  userTimezone: string;
  userLocale: string;
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

export interface ProfileInfoRepresentation {
  name: string;
  disabledFeatures: string[];
  previewFeatures: string[];
  experimentalFeatures: string[];
}

export interface ThemeInfoRepresentation {
  name: string;
  locales: string[];
}

export interface SpiInfoRepresentation {
  internal: boolean;
  providers: { [index: string]: ProviderRepresentation };
}

export interface ProtocolMapperTypeRepresentation {
  id: string;
  name: string;
  category: string;
  helpText: string;
  priority: number;
  properties: ConfigPropertyRepresentation[];
}

export interface ProtocolMapperRepresentation {
  id: string;
  name: string;
  protocol: string;
  protocolMapper: string;
  consentRequired: boolean;
  consentText: string;
  config: { [index: string]: string };
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

export interface ComponentTypeRepresentation {
  id: string;
  helpText: string;
  properties: ConfigPropertyRepresentation[];
  metadata: { [index: string]: any };
}

export interface PasswordPolicyTypeRepresentation {
  id: string;
  displayName: string;
  configType: string;
  defaultValue: string;
  multipleSupported: boolean;
}

export interface ProviderRepresentation {
  order: number;
  operationalInfo: { [index: string]: string };
}

export interface ConfigPropertyRepresentation {
  name: string;
  label: string;
  helpText: string;
  type: string;
  defaultValue: any;
  options: string[];
  secret: boolean;
}
