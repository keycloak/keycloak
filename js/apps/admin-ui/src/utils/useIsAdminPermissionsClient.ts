import { useRealm } from "../context/realm-context/RealmContext";

export function useIsAdminPermissionsClient(selectedClientId?: string) {
  const { realmRepresentation } = useRealm();

  return (
    !!selectedClientId &&
    selectedClientId === realmRepresentation.adminPermissionsClient?.id
  );
}
