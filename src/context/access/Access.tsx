import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";
import React, { createContext, useEffect, useState } from "react";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import useRequiredContext from "../../utils/useRequiredContext";

type AccessContextProps = {
  hasAccess: (...types: AccessType[]) => boolean;
  hasSomeAccess: (...types: AccessType[]) => boolean;
};

export const AccessContext = createContext<AccessContextProps | undefined>(
  undefined
);

export const useAccess = () => useRequiredContext(AccessContext);

type AccessProviderProps = { children: React.ReactNode };
export const AccessContextProvider = ({ children }: AccessProviderProps) => {
  const { whoAmI } = useWhoAmI();
  const { realm } = useRealm();
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
