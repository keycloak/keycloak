import { createNamedContext } from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useContext, useState } from "react";

type LogoContextProps = {
  logo: string;
  setLogo: (logo: string) => void;
};

export const LogoPreviewContext = createNamedContext<
  LogoContextProps | undefined
>("LogoContext", undefined);

export const usePreviewLogo = () => useContext(LogoPreviewContext);

export const LogoContext = ({ children }: PropsWithChildren) => {
  const [logo, setLogo] = useState("");

  return (
    <LogoPreviewContext.Provider value={{ logo, setLogo }}>
      {children}
    </LogoPreviewContext.Provider>
  );
};
