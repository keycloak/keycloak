import {
  Button,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListToggle,
  Spinner,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { getApplications } from "../api/methods";
import { ClientRepresentation } from "../api/representations";
import { Page } from "../components/page/Page";
import { useEnvironment } from "../root/KeycloakContext";
import { usePromise } from "../utils/usePromise";

type Application = ClientRepresentation & {
  open: boolean;
};

export const Applications = () => {
  const { t } = useTranslation();
  const context = useEnvironment();

  const [applications, setApplications] = useState<Application[]>();
  const [key] = useState(1);

  usePromise(
    (signal) => getApplications({ signal, context }),
    (clients) =>
      setApplications(
        clients.map((c) => ({
          ...c,
          open: false,
        })),
      ),
    [key],
  );

  if (!applications) {
    return <Spinner />;
  }

  return (
    <Page title={t("application")} description={t("applicationsIntroMessage")}>
      <DataList id="applications-list" aria-label={t("application")}>
        <DataListItem
          id="applications-list-header"
          aria-labelledby="Columns names"
        >
          <DataListItemRow>
            <span style={{ visibility: "hidden", height: 55 }}>
              <DataListToggle
                id="applications-list-header-invisible-toggle"
                aria-controls="applications-list-content"
              />
            </span>
            <DataListItemCells
              dataListCells={[
                <DataListCell
                  key="applications-list-client-id-header"
                  width={5}
                  className="pf-u-pt-md"
                >
                  <strong>{t("name")}</strong>
                </DataListCell>,
                <DataListCell
                  key="applications-list-status"
                  width={2}
                  className="pf-u-pt-md"
                >
                  <strong>Link</strong>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        {applications.map((application) => (
          <DataListItem
            key={application.clientId}
            aria-labelledby="applications-list"
            isExpanded={application.open}
          >
            <DataListItemRow className="pf-u-align-items-center">
              <DataListItemCells
                className="pf-u-align-items-center"
                dataListCells={[
                  <DataListCell width={5} key={`client${application.clientId}`}>
                    <span>
                      {application.clientName || application.clientId}
                    </span>
                  </DataListCell>,
                  <DataListCell width={2} key={`status${application.clientId}`}>
                    {application.effectiveUrl && (
                      <a href={application.effectiveUrl}>
                        <Button variant="primary">Acessar plataforma</Button>
                      </a>
                    )}
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
    </Page>
  );
};

export default Applications;
