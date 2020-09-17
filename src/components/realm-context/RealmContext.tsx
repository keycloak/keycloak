import React, { useState } from "react";

export const RealmContext = React.createContext({
  realm: "master",
  setRealm: (realm: string) => {},
});

type RealmContextProviderProps = { children: React.ReactNode };

export const RealmContextProvider = ({
  children,
}: RealmContextProviderProps) => {
  const [realm, setRealm] = useState("master");

  return (
    <RealmContext.Provider value={{ realm, setRealm }}>
      {children}
    </RealmContext.Provider>
  );
};
