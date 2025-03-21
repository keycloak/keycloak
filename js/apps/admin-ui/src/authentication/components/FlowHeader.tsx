import { DataListDragButton } from "@patternfly/react-core";
import { Th, Tr } from "@patternfly/react-table";
import { useTranslation } from "react-i18next";

import "./flow-header.css";

export const FlowHeader = () => {
  const { t } = useTranslation();
  return (
    <Tr aria-labelledby="headerName" id="header">
      <Th>
        <DataListDragButton
          className="keycloak__authentication__header-drag-button"
          aria-label={t("disabled")}
        />
        <Th screenReaderText={t("expandRow")} />
      </Th>
      <Th>{t("steps")}</Th>
      <Th>{t("requirement")}</Th>
      <Th screenReaderText={t("config")}></Th>
      <Th screenReaderText={t("config")}></Th>
      <Th screenReaderText={t("config")}></Th>
      <Th screenReaderText={t("config")}></Th>
    </Tr>
  );
};
