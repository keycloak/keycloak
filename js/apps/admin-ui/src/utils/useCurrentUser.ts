import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useState } from "react";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useWhoAmI } from "../context/whoami/WhoAmI";

export function useCurrentUser() {
  const { whoAmI } = useWhoAmI();
  const { adminClient } = useAdminClient();
  const [currentUser, setCurrentUser] = useState<UserRepresentation>();

  const userId = whoAmI.getUserId();

  useFetch(() => adminClient.users.findOne({ id: userId }), setCurrentUser, [
    userId,
  ]);

  return currentUser;
}
