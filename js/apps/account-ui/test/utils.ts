import { generatePath } from "react-router-dom";

import { DEFAULT_REALM, ROOT_PATH, SERVER_URL } from "./constants";

export function getAccountUrl() {
  return SERVER_URL + getRootPath();
}

export function getAdminUrl() {
  return SERVER_URL + "/admin/master/console/";
}

export const getRootPath = (realm = DEFAULT_REALM) =>
  generatePath(ROOT_PATH, { realm });
