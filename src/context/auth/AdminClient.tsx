import { createContext, useContext } from "react";
import KeycloakAdminClient from "keycloak-admin";

export const AdminClient = createContext<KeycloakAdminClient | undefined>(
  undefined
);

export const useAdminClient = () => {
  return useContext(AdminClient)!;
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
 * @param onError custom error handler
 */
export function asyncStateFetch<T>(
  adminClientCall: () => Promise<T>,
  callback: (param: T) => void,
  onError: (error: Error) => void
) {
  let canceled = false;

  adminClientCall()
    .then((result) => {
      try {
        if (!canceled) {
          callback(result);
        }
      } catch (error) {
        if (onError) onError(error);
      }
    })
    .catch(onError);

  return () => {
    canceled = true;
  };
}
