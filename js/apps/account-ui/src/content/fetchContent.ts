import { CallOptions } from "../api/methods";
import { parseResponse } from "../api/parse-response";
import { request } from "../api/request";
import { MenuItem } from "../root/PageNav";

export default async function fetchContentJson(
  opts: CallOptions,
): Promise<MenuItem[]> {
  const response = await request("/content.json", opts.context, {
    signal: opts.signal,
  });
  return parseResponse<MenuItem[]>(response);
}
