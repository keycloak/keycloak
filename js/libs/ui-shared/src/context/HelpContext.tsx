import { PropsWithChildren } from "react";
import { createNamedContext } from "../utils/createNamedContext";
import { useRequiredContext } from "../utils/useRequiredContext";
import { useStoredState } from "../utils/useStoredState";

type HelpContextProps = {
  enabled: boolean;
  toggleHelp: () => void;
};

export const HelpContext = createNamedContext<HelpContextProps | undefined>(
  "HelpContext",
  undefined,
);

export const useHelp = () => useRequiredContext(HelpContext);

export const Help = ({ children }: PropsWithChildren) => {
  const [enabled, setHelp] = useStoredState(localStorage, "helpEnabled", true);

  function toggleHelp() {
    setHelp(!enabled);
  }

  return (
    <HelpContext.Provider value={{ enabled, toggleHelp }}>
      {children}
    </HelpContext.Provider>
  );
};
