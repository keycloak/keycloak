import { Th, Tr } from "@patternfly/react-table";
import { useTranslation } from "react-i18next";

export const FlowHeader = () => {
  const { t } = useTranslation();
  return (
    <Tr aria-labelledby="headerName" id="header">
      <Th
        className="keycloak__authentication__drag-cell"
        screenReaderText={t("dragHandle")}
      />
      <Th>{t("steps")}</Th>
      <Th>{t("requirement")}</Th>
      <Th screenReaderText={t("config")} />
      <Th screenReaderText={t("config")} />
      <Th screenReaderText={t("config")} />
      <Th screenReaderText={t("config")} />
    </Tr>
  );
};
