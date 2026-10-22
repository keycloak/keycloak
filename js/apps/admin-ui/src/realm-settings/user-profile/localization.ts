import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type { Dispatch, SetStateAction } from "react";

type DeleteLocalizationKeysProps = {
  adminClient: KeycloakAdminClient;
  realm: string;
  locales: string[];
  translationValues: Array<string | undefined>;
  setTableData: Dispatch<SetStateAction<Record<string, string>[] | undefined>>;
};

export const getTranslationKey = (value?: string): string | undefined => {
  if (!value?.startsWith("${") || !value.endsWith("}")) {
    return undefined;
  }

  const translationKey = value.slice(2, -1);
  return translationKey.trim().length > 0 ? translationKey : undefined;
};

export const deleteLocalizationKeys = async ({
  adminClient,
  realm,
  locales,
  translationValues,
  setTableData,
}: DeleteLocalizationKeysProps) => {
  const keysToDelete = [
    ...new Set(
      translationValues
        .map(getTranslationKey)
        .filter((key): key is string => Boolean(key)),
    ),
  ];

  if (keysToDelete.length === 0) {
    return;
  }

  await Promise.all(
    locales.map(async (locale) => {
      try {
        const localeMessages =
          (await adminClient.realms.getRealmLocalizationTexts({
            realm,
            selectedLocale: locale,
          })) as Record<string, string> | undefined;

        if (!localeMessages) {
          return;
        }

        const localeKeysToDelete = keysToDelete.filter(
          (key) => key in localeMessages,
        );
        if (localeKeysToDelete.length === 0) {
          return;
        }

        await Promise.all(
          localeKeysToDelete.map((key) =>
            adminClient.realms.deleteRealmLocalizationTexts({
              realm,
              selectedLocale: locale,
              key,
            }),
          ),
        );

        const updatedData = await adminClient.realms.getRealmLocalizationTexts({
          realm,
          selectedLocale: locale,
        });
        setTableData([updatedData]);
      } catch {
        console.error(`Error removing translations for ${locale}`);
      }
    }),
  );
};
