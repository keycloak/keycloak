import { createContext, useContext } from "react";
import KeycloakAdminClient from "keycloak-admin";
import { RealmContext } from "../realm-context/RealmContext";

export const AdminClient = createContext<KeycloakAdminClient | undefined>(
  undefined
);

export const useAdminClient = () => {
  const adminClient = useContext(AdminClient)!;
  const { realm } = useContext(RealmContext);

  adminClient.setConfig({
    realmName: realm,
  });

  return adminClient;
};

/**
 * Util function to only set the state when the component is still mounted.
 *
 * It takes 2 functions one you do your adminClient call in and the other to set your state
 *
 * @example
 * useEffect(() => {
 *   return asyncStateFetch(
 *     () => adminClient.components.findOne({ id }),
 *     (component) => setupForm(component)
 *   );
 * }, []);
 *
 * @param adminClientCall use this to do your adminClient call
 * @param callback when the data is fetched this is where you set your state
 */
export function asyncStateFetch<T>(
  adminClientCall: () => Promise<T>,
  callback: (param: T) => void
) {
  let canceled = false;

  adminClientCall().then((result) => {
    if (!canceled) {
      callback(result);
    }
  });

  return () => {
    canceled = true;
  };
}
