import { PropsWithChildren, useEffect, useMemo } from "react";
import { useMatch } from "react-router-dom";

import { DashboardRouteWithRealm } from "../../dashboard/routes/Dashboard";
import environment from "../../environment";
import { createNamedContext, useRequiredContext } from "ui-shared";
import { useAdminClient } from "../auth/AdminClient";

type RealmContextType = {
  realm: string;
};

export const RealmContext = createNamedContext<RealmContextType | undefined>(
  "RealmContext",
  undefined
);

export const RealmContextProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const routeMatch = useMatch({
    path: DashboardRouteWithRealm.path,
    end: false,
  });

  const realmParam = routeMatch?.params.realm;
  const realm = useMemo(
    () => realmParam ?? environment.loginRealm,
    [realmParam]
  );

  // Configure admin client to use selected realm when it changes.
  useEffect(() => adminClient.setConfig({ realmName: realm }), [realm]);

  const value = useMemo(() => ({ realm }), [realm]);

  return (
    <RealmContext.Provider value={value}>{children}</RealmContext.Provider>
  );
};

export const useRealm = () => useRequiredContext(RealmContext);
