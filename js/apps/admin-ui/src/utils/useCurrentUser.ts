import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useState } from "react";

import { adminClient } from "../admin-client";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { useFetch } from "./useFetch";

export function useCurrentUser() {
  const { whoAmI } = useWhoAmI();
  const [currentUser, setCurrentUser] = useState<UserRepresentation>();

  const userId = whoAmI.getUserId();

  useFetch(() => adminClient.users.findOne({ id: userId }), setCurrentUser, [
    userId,
  ]);

  return currentUser;
}
