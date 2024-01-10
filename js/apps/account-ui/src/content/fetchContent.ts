import { CallOptions } from "../api/methods";
import { environment } from "../environment";
import { MenuItem } from "../root/PageNav";
import { joinPath } from "../utils/joinPath";

export default async function fetchContentJson(
  opts: CallOptions = {},
): Promise<MenuItem[]> {
  const response = await fetch(
    joinPath(environment.resourceUrl, "/content.json"),
    opts,
  );
  return await response.json();
}
