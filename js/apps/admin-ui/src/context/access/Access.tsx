import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import { PropsWithChildren, useEffect, useState } from "react";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import {
  createNamedContext,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";

type AccessContextProps = {
  hasAccess: (...types: AccessType[]) => boolean;
  hasSomeAccess: (...types: AccessType[]) => boolean;
};

export const AccessContext = createNamedContext<AccessContextProps | undefined>(
  "AccessContext",
  undefined,
);

export const useAccess = () => useRequiredContext(AccessContext);

export const AccessContextProvider = ({ children }: PropsWithChildren) => {
  const { whoAmI } = useWhoAmI();
  const { realm } = useRealm();
  const [access, setAccess] = useState<readonly AccessType[]>([]);

  useEffect(() => {
    if (whoAmI.getRealmAccess()[realm]) {
      setAccess(whoAmI.getRealmAccess()[realm]);
    }
  }, [whoAmI, realm]);

  const hasAccess = (...types: AccessType[]): boolean => {
    return types.every(
      (type) =>
        type === "anyone" ||
        (typeof type === "function" &&
          type({ hasAll: hasAccess, hasAny: hasSomeAccess })) ||
        access.includes(type),
    );
  };

  const hasSomeAccess = (...types: AccessType[]): boolean => {
    return types.some(
      (type) =>
        type === "anyone" ||
        (typeof type === "function" &&
          type({ hasAll: hasAccess, hasAny: hasSomeAccess })) ||
        access.includes(type),
    );
  };

  return (
    <AccessContext.Provider value={{ hasAccess, hasSomeAccess }}>
      {children}
    </AccessContext.Provider>
  );
};
