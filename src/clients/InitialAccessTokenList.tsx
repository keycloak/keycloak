import React from "react";

import { useAdminClient } from "../context/auth/AdminClient";

export const InitialAccessTokenList = () => {
  const adminClient = useAdminClient();
  return <h1>Hello</h1>;
};
