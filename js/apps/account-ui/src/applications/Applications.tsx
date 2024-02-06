import {
  Button,
  DataList,
  DataListCell,
  DataListContent,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListToggle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
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

function filterRestrictClientIds(c: ClientRepresentation) {
  return (
    c.clientId !== "account" &&
    c.clientId !== "account-console" &&
    c.clientId !== "admin-cli" &&
    c.clientId !== "broker" &&
    c.clientId !== "master-realm" &&
    c.clientId !== "security-admin-console" &&
    c.clientId !== "security-admin-console-v2"
  );
}

export const Applications = () => {
  const { t } = useTranslation();
  const context = useEnvironment();

  const [applications, setApplications] = useState<Application[]>();
  const [key] = useState(1);

  usePromise(
    (signal) => getApplications({ signal, context }),
    (clients) =>
      setApplications(
        clients.filter(filterRestrictClientIds).map((c) => ({
          ...c,
          open: false,
        })),
      ),
    [key],
  );

  const toggleOpen = (clientId: string) => {
    setApplications([
      ...applications!.map((a) =>
        a.clientId === clientId ? { ...a, open: !a.open } : a,
      ),
    ]);
  };

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
                  width={2}
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
              <DataListToggle
                onClick={() => toggleOpen(application.clientId)}
                isExpanded={application.open}
                id={`toggle-${application.clientId}`}
                aria-controls={`content-${application.clientId}`}
              />
              <DataListItemCells
                className="pf-u-align-items-center"
                dataListCells={[
                  <DataListCell width={2} key={`client${application.clientId}`}>
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

            <DataListContent
              id={`content-${application.clientId}`}
              className="pf-u-pl-4xl"
              aria-label={t("applicationDetails", {
                clientId: application.clientId,
              })}
              isHidden={!application.open}
            >
              <DescriptionList>
                {application.description && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>
                      {t("description")}
                    </DescriptionListTerm>
                    <DescriptionListDescription>
                      {application.description}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {application.effectiveUrl && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>URL</DescriptionListTerm>
                    <DescriptionListDescription>
                      <Button
                        className="pf-u-pl-0 title-case"
                        component="a"
                        variant="link"
                        href={application.effectiveUrl}
                      >
                        {application.effectiveUrl.split('"')}
                      </Button>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {application.consent && (
                  <>
                    {application.tosUri && (
                      <DescriptionListGroup>
                        <DescriptionListTerm>
                          {t("termsOfService")}
                        </DescriptionListTerm>
                        <DescriptionListDescription>
                          {application.tosUri}
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    )}
                    {application.policyUri && (
                      <DescriptionListGroup>
                        <DescriptionListTerm>
                          {t("privacyPolicy")}
                        </DescriptionListTerm>
                        <DescriptionListDescription>
                          {application.policyUri}
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    )}
                    {application.logoUri && (
                      <DescriptionListGroup>
                        <DescriptionListTerm>{t("logo")}</DescriptionListTerm>
                        <DescriptionListDescription>
                          <img src={application.logoUri} />
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    )}
                  </>
                )}
              </DescriptionList>
            </DataListContent>
          </DataListItem>
        ))}
      </DataList>
    </Page>
  );
};

export default Applications;
