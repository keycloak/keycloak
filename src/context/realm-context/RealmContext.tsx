import React, { useContext, useState } from "react";

type RealmContextType = {
  realm: string;
  setRealm: (realm: string) => void;
};

export const RealmContext = React.createContext<RealmContextType>({
  realm: "",
  setRealm: () => {},
});

type RealmContextProviderProps = { children: React.ReactNode };

export const RealmContextProvider = ({
  children,
}: RealmContextProviderProps) => {
  const [realm, setRealm] = useState("");

  return (
    <RealmContext.Provider value={{ realm, setRealm }}>
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useContext(RealmContext);
