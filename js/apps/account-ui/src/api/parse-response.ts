import {
  getNetworkErrorMessage,
  getNetworkErrorDescription,
} from "@keycloak/keycloak-ui-shared";
import { CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON } from "./constants";

export class ApiError extends Error {
  description?: string;

  constructor(message: string, description?: string) {
    super(message);
    this.description = description;
  }
}

export async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get(CONTENT_TYPE_HEADER);
  const isJSON = contentType ? contentType.includes(CONTENT_TYPE_JSON) : false;

  if (!isJSON) {
    throw new Error(
      `Expected response to have a JSON content type, got '${contentType}' instead.`,
    );
  }

  const data = await parseJSON(response);

  if (!response.ok) {
    const message = getNetworkErrorMessage(data);
    const description = getNetworkErrorDescription(data);

    if (!message) {
      throw new Error(
        "Unable to retrieve error message from response, no matching key found.",
      );
    }

    throw new ApiError(message, description);
  }

  return data as T;
}

async function parseJSON(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch (error) {
    throw new Error("Unable to parse response as valid JSON.", {
      cause: error,
    });
  }
}
