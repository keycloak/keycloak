import React, { useState, useContext } from "react";
import { WhoAmIContext } from "../../whoami/WhoAmI";

export const RealmContext = React.createContext({
  realm: "",
  setRealm: (realm: string) => {},
});

type RealmContextProviderProps = { children: React.ReactNode };

export const RealmContextProvider = ({
  children,
}: RealmContextProviderProps) => {
  const homeRealm = useContext(WhoAmIContext).getHomeRealm();
  const [realm, setRealm] = useState(homeRealm);

  return (
    <RealmContext.Provider value={{ realm, setRealm }}>
      {children}
    </RealmContext.Provider>
  );
};
