import type { KeycloakAdminClient } from "../client.js";
import { Agent, RequestArgs } from "./agent.js";

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

  public makeRequest = <PayloadType = any, ResponseType = any>(
    args: RequestArgs,
  ): ((
    payload?: PayloadType & ParamType,
    options?: Pick<RequestArgs, "catchNotFound">,
  ) => Promise<ResponseType>) => {
    return this.#agent.request(args);
  };

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
