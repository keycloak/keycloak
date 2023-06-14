import { isRecord } from "../utils/isRecord";
import { CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON } from "./constants";

export class ApiError extends Error {}

export async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get(CONTENT_TYPE_HEADER);
  const isJSON = contentType ? contentType.includes(CONTENT_TYPE_JSON) : false;

  if (!isJSON) {
    throw new Error(
      `Expected response to have a JSON content type, got '${contentType}' instead.`
    );
  }

  const data = await parseJSON(response);

  if (!response.ok) {
    throw new ApiError(getErrorMessage(data));
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

function getErrorMessage(data: unknown): string {
  if (!isRecord(data)) {
    throw new Error("Unable to retrieve error message from response.");
  }

  const errorKeys = ["error_description", "errorMessage", "error"];

  for (const key of errorKeys) {
    const value = data[key];

    if (typeof value === "string") {
      return value;
    }
  }

  throw new Error(
    "Unable to retrieve error message from response, no matching key found."
  );
}
