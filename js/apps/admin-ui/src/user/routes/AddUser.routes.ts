import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type AddUserParams = { realm: string };

export const AddUserRoutePath = "/:realm/users/add-user";

export const toAddUser = (params: AddUserParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddUserRoutePath, params),
});
