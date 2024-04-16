import { generatePath } from "react-router-dom";
import { DEFAULT_REALM, ROOT_PATH } from "../constants";

export const getRootPath = (realm = DEFAULT_REALM) =>
  generatePath(ROOT_PATH, { realm });
