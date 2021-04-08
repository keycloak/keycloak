import React, { useContext, useState } from "react";
import { RecentUsed } from "../../components/realm-selector/recent-used";

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
  const recentUsed = new RecentUsed();

  const set = (realm: string) => {
    recentUsed.setRecentUsed(realm);
    setRealm(realm);
  };

  return (
    <RealmContext.Provider value={{ realm, setRealm: set }}>
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useContext(RealmContext);
