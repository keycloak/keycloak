import { useTranslation } from "react-i18next";

export const RealmOverrides = () => {
  const { t } = useTranslation("realm-settings");

  return <h1>{t("realmOverrides")}</h1>;
};
