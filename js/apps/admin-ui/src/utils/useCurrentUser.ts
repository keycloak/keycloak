import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useAdminClient } from "../admin-client";
import { useWhoAmI } from "../context/whoami/WhoAmI";

export function useCurrentUser() {
  const { adminClient } = useAdminClient();
  const { whoAmI } = useWhoAmI();
  const [currentUser, setCurrentUser] = useState<UserRepresentation>();

  const userId = whoAmI.getUserId();

  useFetch(() => adminClient.users.findOne({ id: userId }), setCurrentUser, [
    userId,
  ]);

  return { ...currentUser, realm: whoAmI.getRealm() };
}
