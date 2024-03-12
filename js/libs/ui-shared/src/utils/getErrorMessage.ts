import { NetworkError } from "@keycloak/keycloak-admin-client";

export function getErrorMessage(error: unknown) {
  if (typeof error === "string") {
    return error;
  }

  if (error instanceof NetworkError) {
    return getNetworkErrorMessage(error);
  }

  if (error instanceof Error) {
    return error.message;
  }
}

function getNetworkErrorMessage({ responseData }: NetworkError) {
  const data = responseData as Record<string, unknown>;

  for (const key of ["error_description", "errorMessage", "error"]) {
    const value = data[key];

    if (typeof value === "string") {
      return value;
    }
  }
}
