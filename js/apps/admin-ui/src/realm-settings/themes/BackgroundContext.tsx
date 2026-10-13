import { createNamedContext } from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useContext, useState } from "react";

type BackgroundContextProps = {
  background: string;
  setBackground: (background: string) => void;
};

export const BackgroundPreviewContext = createNamedContext<
  BackgroundContextProps | undefined
>("BackgroundContext", undefined);

export const usePreviewBackground = () => useContext(BackgroundPreviewContext);

export const BackgroundContext = ({ children }: PropsWithChildren) => {
  const [background, setBackground] = useState("");

  return (
    <BackgroundPreviewContext.Provider value={{ background, setBackground }}>
      {children}
    </BackgroundPreviewContext.Provider>
  );
};
