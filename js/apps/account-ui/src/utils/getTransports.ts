/**
 * Parses JSON and returns the `transports` array if it exists and is valid.
 */
export function getTransports(json?: string): string[] | undefined {
  try {
    const parsed = json ? JSON.parse(json) : undefined;
    return Array.isArray(parsed?.transports) ? parsed.transports : undefined;
  } catch {
    return undefined;
  }
}