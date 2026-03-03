import type { KeycloakAdminClient } from "../client.js";
import { Agent, RequestArgs } from "./agent.js";

type RequestFunction<
  PayloadType,
  ParamType,
  ResponseType,
> = {
  (
    payload?: PayloadType & ParamType,
    options?: { catchNotFound?: false },
  ): Promise<ResponseType>;
  (
    payload?: PayloadType & ParamType,
    options?: { catchNotFound: true },
  ): Promise<ResponseType | null>;
  (
    payload?: PayloadType & ParamType,
    options?: Pick<RequestArgs, "catchNotFound">,
  ): Promise<ResponseType | null>;
};

type RequestFunctionWithDefaultCatchNotFound<
  PayloadType,
  ParamType,
  ResponseType,
> = (
  payload?: PayloadType & ParamType,
  options?: Pick<RequestArgs, "catchNotFound">,
) => Promise<ResponseType | null>;

export default class Resource<ParamType = {}> {
  #agent: Agent;
  constructor(
    client: KeycloakAdminClient,
    settings: {
      path?: string;
      getUrlParams?: () => Record<string, any>;
      getBaseUrl?: () => string;
    } = {},
  ) {
    this.#agent = new Agent({
      client,
      ...settings,
    });
  }

  public makeRequest<PayloadType = any, ResponseType = any>(
    args: RequestArgs & { catchNotFound: true },
  ): RequestFunctionWithDefaultCatchNotFound<
    PayloadType,
    ParamType,
    ResponseType
  >;
  public makeRequest<PayloadType = any, ResponseType = any>(
    args: RequestArgs,
  ): RequestFunction<PayloadType, ParamType, ResponseType>;
  public makeRequest<PayloadType = any, ResponseType = any>(
    args: RequestArgs,
  ) {
    return this.#agent.request(args);
  }

  // update request will take three types: query, payload and response
  public makeUpdateRequest = <
    QueryType = any,
    PayloadType = any,
    ResponseType = any,
  >(
    args: RequestArgs,
  ): ((
    query: QueryType & ParamType,
    payload: PayloadType,
  ) => Promise<ResponseType>) => {
    return this.#agent.updateRequest(args);
  };
}
