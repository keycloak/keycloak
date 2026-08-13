/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_systeminforepresentation
 */

export default interface SystemInfoRepresentation {
  version?: string;
  serverTime?: string;
  uptime?: string;
  uptimeMillis?: number;
  javaVersion?: string;
  javaVendor?: string;
  javaVm?: string;
  javaVmVersion?: string;
  javaRuntime?: string;
  javaHome?: string;
  osName?: string;
  osArchitecture?: string;
  osVersion?: string;
  fileEncoding?: string;
  userName?: string;
  userDir?: string;
  userTimezone?: string;
  userLocale?: string;
}
