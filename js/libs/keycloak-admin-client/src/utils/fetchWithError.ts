const ERROR_FIELDS = ["error", "errorMessage"];
const WARNED_DEPRECATED_ENDPOINTS = new Set<string>();

function isDeprecatedResponse(response: Response): boolean {
  const deprecationHeader = response.headers.get("deprecation");
  const warningHeader = response.headers.get("warning");

  const hasDeprecationHeader =
    typeof deprecationHeader === "string" &&
    deprecationHeader.toLowerCase() !== "false";
  const warningMentionsDeprecation =
    typeof warningHeader === "string" &&
    warningHeader.toLowerCase().includes("deprecat");

  return hasDeprecationHeader || warningMentionsDeprecation;
}

function resolveRequestUrl(
  input: Request | string | URL,
  response: Response,
): URL | undefined {
  try {
    if (response.url) {
      return new URL(response.url);
    }

    if (typeof input === "string") {
      return new URL(input);
    }

    if (input instanceof URL) {
      return input;
    }

    return new URL(input.url);
  } catch {
    return undefined;
  }
}

function extractDeprecationLink(linkHeader: string | null): string | undefined {
  if (!linkHeader) {
    return undefined;
  }

  const match = /<([^>]+)>;\s*rel="?deprecation"?/i.exec(linkHeader);
  return match?.[1];
}

function warnIfDeprecatedEndpoint(
  input: Request | string | URL,
  response: Response,
) {
  if (!isDeprecatedResponse(response)) {
    return;
  }

  const url = resolveRequestUrl(input, response);
  if (!url?.pathname.includes("/admin/")) {
    return;
  }

  const endpointKey = `${url.origin}${url.pathname}`;
  if (WARNED_DEPRECATED_ENDPOINTS.has(endpointKey)) {
    return;
  }
  WARNED_DEPRECATED_ENDPOINTS.add(endpointKey);

  const details: string[] = [];
  const deprecationHeader = response.headers.get("deprecation");
  const warningHeader = response.headers.get("warning");
  const sunsetHeader = response.headers.get("sunset");
  const deprecationLink = extractDeprecationLink(response.headers.get("link"));

  if (deprecationHeader) {
    details.push(`Deprecation: ${deprecationHeader}`);
  }
  if (warningHeader) {
    details.push(`Warning: ${warningHeader}`);
  }
  if (sunsetHeader) {
    details.push(`Sunset: ${sunsetHeader}`);
  }
  if (deprecationLink) {
    details.push(`Deprecation-Info: ${deprecationLink}`);
  }

  const suffix = details.length > 0 ? ` (${details.join(", ")})` : "";
  console.warn(
    `Using deprecated Keycloak Admin endpoint: ${url.pathname}${suffix}.`,
  );
}

export type NetworkErrorOptions = { response: Response; responseData: unknown };

export class NetworkError extends Error {
  response: Response;
  responseData: unknown;

  constructor(message: string, options: NetworkErrorOptions) {
    super(message);
    this.response = options.response;
    this.responseData = options.responseData;
  }
}

export async function fetchWithError(
  input: Request | string | URL,
  init?: RequestInit,
) {
  const response = await fetch(input, init);

  if (!response.ok) {
    const responseData = await parseResponse(response);
    const message = getErrorMessage(responseData);
    console.error(message, response.status, responseData);
    throw new NetworkError(message, {
      response,
      responseData,
    });
  }

  warnIfDeprecatedEndpoint(input, response);
  return response;
}

export async function parseResponse(response: Response): Promise<any> {
  if (!response.body) {
    return "";
  }

  const data = await response.text();

  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
}

function getErrorMessage(data: unknown): string {
  if (typeof data !== "object" || data === null) {
    return "Unable to determine error message.";
  }

  for (const key of ERROR_FIELDS) {
    const value = (data as Record<string, unknown>)[key];

    if (typeof value === "string") {
      return value;
    }
  }

  return "Network response was not OK.";
}
