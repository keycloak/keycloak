import { useTranslation } from "react-i18next";
import {
  DataListItem,
  DataListItemRow,
  DataListDragButton,
  DataListItemCells,
  DataListCell,
} from "@patternfly/react-core";

import "./flow-header.css";

export const FlowHeader = () => {
  const { t } = useTranslation();
  return (
    <DataListItem aria-labelledby="headerName" id="header">
      <DataListItemRow>
        <DataListDragButton
          className="keycloak__authentication__header-drag-button"
          aria-label={t("disabled")}
        />
        <DataListItemCells
          className="keycloak__authentication__header"
          dataListCells={[
            <DataListCell key="step" id="headerName">
              {t("steps")}
            </DataListCell>,
            <DataListCell key="requirement">{t("requirement")}</DataListCell>,
            <DataListCell key="config"></DataListCell>,
          ]}
        />
      </DataListItemRow>
    </DataListItem>
  );
};
