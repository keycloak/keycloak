type ResponseData =
  | string
  | number
  | boolean
  | null
  | ResponseData[]
  | { [key: string]: ResponseData };

export type NetworkErrorOptions = {
  response: Response;
  responseData: ResponseData;
};

export class NetworkError extends Error {
  response: Response;
  responseData: ResponseData;

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
    throw new NetworkError("Network response was not OK.", {
      response,
      responseData,
    });
  }

  return response;
}

export async function parseResponse(response: Response): Promise<ResponseData> {
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
