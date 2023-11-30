import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

export const getUnmanagedAttributes = (
  id: string,
): Promise<Record<string, string[]> | undefined> =>
  fetchAdminUI(`ui-ext/users/${id}/unmanagedAttributes`);
