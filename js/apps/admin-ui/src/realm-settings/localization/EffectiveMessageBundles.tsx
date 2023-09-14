import { useTranslation } from "react-i18next";

export const EffectiveMessageBundles = () => {
  const { t } = useTranslation();

  return <h1>{t("effectiveMessageBundles")}</h1>;
};
