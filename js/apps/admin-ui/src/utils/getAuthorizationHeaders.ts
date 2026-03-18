export function getAuthorizationHeaders(
  accessToken?: string,
): Record<string, string> {
  if (!accessToken) {
    return {};
  }

  return { Authorization: `Bearer ${accessToken}` };
}
