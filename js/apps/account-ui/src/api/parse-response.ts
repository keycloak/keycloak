import { getErrorMessage } from "ui-shared";
import { CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON } from "./constants";

export class ApiError extends Error {}

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
    throw new ApiError(getErrorMessage(data) ?? "An unknown error occurred.");
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
