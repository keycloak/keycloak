export function getBaseUrl(): string {
  return process.env.KEYCLOAK_SERVER ?? "http://localhost:8080";
}
