import { useMemo } from "react";
import { useRealm } from "../context/realm-context/RealmContext";
import { DEFAULT_LOCALE } from "../i18n/i18n";

export default function useLocale() {
  const { realmRepresentation: realm } = useRealm();

  const defaultSupportedLocales = useMemo(() => {
    return realm?.supportedLocales?.length
      ? realm.supportedLocales
      : [DEFAULT_LOCALE];
  }, [realm]);

  const defaultLocales = useMemo(() => {
    return realm?.defaultLocale?.length ? [realm.defaultLocale] : [];
  }, [realm]);

  const combinedLocales = useMemo(() => {
    return Array.from(new Set([...defaultLocales, ...defaultSupportedLocales]));
  }, [defaultLocales, defaultSupportedLocales]);

  return combinedLocales;
}
