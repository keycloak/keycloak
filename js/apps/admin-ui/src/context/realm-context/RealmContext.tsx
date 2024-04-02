import { PropsWithChildren, useEffect, useMemo } from "react";
import { useMatch } from "react-router-dom";
import { createNamedContext, useRequiredContext } from "ui-shared";

import { adminClient } from "../../admin-client";
import { DashboardRouteWithRealm } from "../../dashboard/routes/Dashboard";
import environment from "../../environment";

type RealmContextType = {
  realm: string;
};

export const RealmContext = createNamedContext<RealmContextType | undefined>(
  "RealmContext",
  undefined,
);

export const RealmContextProvider = ({ children }: PropsWithChildren) => {
  const routeMatch = useMatch({
    path: DashboardRouteWithRealm.path,
    end: false,
  });

  const realmParam = routeMatch?.params.realm;
  const realm = useMemo(
    () => decodeURIComponent(realmParam ?? environment.loginRealm),
    [realmParam],
  );

  // Configure admin client to use selected realm when it changes.
  useEffect(() => adminClient.setConfig({ realmName: realm }), [realm]);

  const value = useMemo(() => ({ realm }), [realm]);

  return (
    <RealmContext.Provider value={value}>{children}</RealmContext.Provider>
  );
};

export const useRealm = () => useRequiredContext(RealmContext);
