import { environment } from "../environment.js";
import { joinPath } from "../utils/join-path.js";

/**
 * @param {AbortSignal} [signal]
 * @returns {Promise<import("../types/menu.js").MenuItem[]>}
 */
export async function fetchContentJson(signal) {
  const response = await fetch(
    joinPath(environment.resourceUrl, "/content.json"),
    { signal },
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch content.json: ${response.status}`);
  }

  return response.json();
}
