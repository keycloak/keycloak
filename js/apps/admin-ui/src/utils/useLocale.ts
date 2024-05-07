import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useMemo, useState } from "react";
import { DEFAULT_LOCALE } from "../i18n/i18n";
import { useFetch } from "./useFetch";
import { useRealm } from "../context/realm-context/RealmContext";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";

export default function useLocale() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      if (!realm) {
        throw new Error(t("notFound"));
      }
      setRealm(realm);
    },
    []
  );
  const defaultSupportedLocales = useMemo(() => {
    return realm?.supportedLocales?.length ? realm.supportedLocales : [DEFAULT_LOCALE];
  }, [realm]);

  const defaultLocales = useMemo(() => {
    return realm?.defaultLocale?.length ? [realm.defaultLocale] : [];
  }, [realm]);

  const combinedLocales = useMemo(() => {
    return Array.from(new Set([...defaultLocales, ...defaultSupportedLocales]));
  }, [defaultLocales, defaultSupportedLocales]);

  return combinedLocales;
}
