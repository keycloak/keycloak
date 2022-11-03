import { createContext, FunctionComponent, useContext } from "react";

import type { Translations } from "./translations";
import { defaultTranslations } from "./translations";

const TranslationsContext = createContext<Translations>(defaultTranslations);

export const useTranslations = () => useContext(TranslationsContext);

export type TranslationsProviderProps = {
  translations: Translations;
};

export const TranslationsProvider: FunctionComponent<
  TranslationsProviderProps
> = ({ translations, children }) => {
  return (
    <TranslationsContext.Provider value={translations}>
      {children}
    </TranslationsContext.Provider>
  );
};
