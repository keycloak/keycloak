import React, { createContext, useContext, useEffect, useState } from "react";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";

import { RealmContext } from "../../context/realm-context/RealmContext";
import { WhoAmIContext } from "../../context/whoami/WhoAmI";

type AccessContextProps = {
  hasAccess: (...types: AccessType[]) => boolean;
  hasSomeAccess: (...types: AccessType[]) => boolean;
};

export const AccessContext = createContext<AccessContextProps>({
  hasAccess: () => false,
  hasSomeAccess: () => false,
});

export const useAccess = () => useContext(AccessContext);

type AccessProviderProps = { children: React.ReactNode };
export const AccessContextProvider = ({ children }: AccessProviderProps) => {
  const { whoAmI } = useContext(WhoAmIContext);
  const { realm } = useContext(RealmContext);
  const [access, setAccess] = useState<readonly AccessType[]>([]);

  useEffect(() => {
    if (whoAmI.getRealmAccess()[realm]) {
      setAccess(whoAmI.getRealmAccess()[realm]);
    }
  }, [whoAmI, realm]);

  const hasAccess = (...types: AccessType[]) => {
    return types.every((type) => type === "anyone" || access.includes(type));
  };

  const hasSomeAccess = (...types: AccessType[]) => {
    return types.some((type) => type === "anyone" || access.includes(type));
  };

  return (
    <AccessContext.Provider value={{ hasAccess, hasSomeAccess }}>
      {children}
    </AccessContext.Provider>
  );
};
