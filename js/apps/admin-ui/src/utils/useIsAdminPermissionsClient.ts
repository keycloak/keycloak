import { useState, useEffect } from "react";
import { useRealm } from "../context/realm-context/RealmContext";

export function useIsAdminPermissionsClient(selectedClientId: string) {
  const { realmRepresentation } = useRealm();
  const [isAdminPermissionsClient, setIsAdminPermissionsClient] =
    useState<boolean>(false);

  useEffect(() => {
    if (realmRepresentation?.adminPermissionsClient) {
      setIsAdminPermissionsClient(
        selectedClientId === realmRepresentation.adminPermissionsClient.id,
      );
    } else {
      setIsAdminPermissionsClient(false);
    }
  }, [selectedClientId, realmRepresentation]);

  return isAdminPermissionsClient;
}
