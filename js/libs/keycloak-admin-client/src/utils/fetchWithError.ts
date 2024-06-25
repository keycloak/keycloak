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
  input: RequestInfo | URL,
  init?: RequestInit,
) {
  const response = await fetch(input, init);

  if (!response.ok) {
    const responseData = await parseResponse(response);
    throw new NetworkError(responseData, {
      response,
      responseData,
    });
  }

  return response;
}

export async function parseResponse(response: Response): Promise<any> {
  if (!response.body) {
    return "";
  }

  const data = await response.text();

  try {
    return JSON.parse(data);
    // eslint-disable-next-line no-empty
  } catch (error) {}

  return data;
}
