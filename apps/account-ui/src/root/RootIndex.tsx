import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

export const RootIndex = () => {
  const { t } = useTranslation();
  return <PageSection>{t("welcomeMessage")}</PageSection>;
};
