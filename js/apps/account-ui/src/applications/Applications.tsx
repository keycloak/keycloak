import {
  ContinueCancelModal,
  label,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
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
  Grid,
  GridItem,
  Spinner,
} from "@patternfly/react-core";
import {
  CheckIcon,
  ExternalLinkAltIcon,
  InfoAltIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { deleteConsent, getApplications } from "../api/methods";
import { ClientRepresentation } from "../api/representations";
import { Page } from "../components/page/Page";
import { TFuncKey } from "../i18n";
import { formatDate } from "../utils/formatDate";
import { useAccountAlerts } from "../utils/useAccountAlerts";
import { usePromise } from "../utils/usePromise";

type Application = ClientRepresentation & {
  open: boolean;
};

export const Applications = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();

  const [applications, setApplications] = useState<Application[]>();
  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise(
    (signal) => getApplications({ signal, context }),
    (clients) => setApplications(clients.map((c) => ({ ...c, open: false }))),
    [key],
  );

  const toggleOpen = (clientId: string) => {
    setApplications([
      ...applications!.map((a) =>
        a.clientId === clientId ? { ...a, open: !a.open } : a,
      ),
    ]);
  };

  const removeConsent = async (id: string) => {
    try {
      await deleteConsent(context, id);
      refresh();
      addAlert(t("removeConsentSuccess"));
    } catch (error) {
      addError("removeConsentError", error);
    }
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
                  className="pf-v5-u-pt-md"
                >
                  <strong>{t("name")}</strong>
                </DataListCell>,
                <DataListCell
                  key="applications-list-app-type-header"
                  width={2}
                  className="pf-v5-u-pt-md"
                >
                  <strong>{t("applicationType")}</strong>
                </DataListCell>,
                <DataListCell
                  key="applications-list-status"
                  width={2}
                  className="pf-v5-u-pt-md"
                >
                  <strong>{t("status")}</strong>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        {applications.map((application) => (
          <DataListItem
            key={application.clientId}
            aria-labelledby="applications-list"
            data-testid="applications-list-item"
            isExpanded={application.open}
          >
            <DataListItemRow className="pf-v5-u-align-items-center">
              <DataListToggle
                onClick={() => toggleOpen(application.clientId)}
                isExpanded={application.open}
                id={`toggle-${application.clientId}`}
                aria-controls={`content-${application.clientId}`}
              />
              <DataListItemCells
                className="pf-v5-u-align-items-center"
                dataListCells={[
                  <DataListCell width={2} key={`client${application.clientId}`}>
                    {application.effectiveUrl && (
                      <Button
                        className="pf-v5-u-pl-0 title-case"
                        component="a"
                        variant="link"
                        onClick={() => window.open(application.effectiveUrl)}
                      >
                        {label(
                          t,
                          application.clientName || application.clientId,
                        )}{" "}
                        <ExternalLinkAltIcon />
                      </Button>
                    )}
                    {!application.effectiveUrl && (
                      <>
                        {label(
                          t,
                          application.clientName || application.clientId,
                        )}
                      </>
                    )}
                  </DataListCell>,
                  <DataListCell
                    width={2}
                    key={`internal${application.clientId}`}
                  >
                    {application.userConsentRequired
                      ? t("thirdPartyApp")
                      : t("internalApp")}
                    {application.offlineAccess ? ", " + t("offlineAccess") : ""}
                  </DataListCell>,
                  <DataListCell width={2} key={`status${application.clientId}`}>
                    {application.inUse ? t("inUse") : t("notInUse")}
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>

            <DataListContent
              id={`content-${application.clientId}`}
              className="pf-v5-u-pl-4xl"
              aria-label={t("applicationDetails", {
                clientId: application.clientId,
              })}
              isHidden={!application.open}
            >
              <DescriptionList>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("client")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    {application.clientId}
                  </DescriptionListDescription>
                </DescriptionListGroup>
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
                      {application.effectiveUrl.split('"')}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {application.consent && (
                  <>
                    <DescriptionListGroup>
                      <DescriptionListTerm>
                        {t("hasAccessTo")}
                      </DescriptionListTerm>
                      {application.consent.grantedScopes.map((scope) => (
                        <DescriptionListDescription key={`scope${scope.id}`}>
                          <CheckIcon />{" "}
                          {t(scope.name as TFuncKey, scope.displayText)}
                        </DescriptionListDescription>
                      ))}
                    </DescriptionListGroup>
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
                    <DescriptionListGroup>
                      <DescriptionListTerm>
                        {t("accessGrantedOn")}
                      </DescriptionListTerm>
                      <DescriptionListDescription>
                        {formatDate(new Date(application.consent.createdDate))}
                      </DescriptionListDescription>
                    </DescriptionListGroup>
                  </>
                )}
              </DescriptionList>
              {(application.consent || application.offlineAccess) && (
                <Grid hasGutter>
                  <hr />
                  <GridItem>
                    <ContinueCancelModal
                      buttonTitle={t("removeAccess")}
                      modalTitle={t("removeAccess")}
                      continueLabel={t("confirm")}
                      cancelLabel={t("cancel")}
                      buttonVariant="secondary"
                      onContinue={() => removeConsent(application.clientId)}
                    >
                      {t("removeModalMessage", { name: application.clientId })}
                    </ContinueCancelModal>
                  </GridItem>
                  <GridItem>
                    <InfoAltIcon /> {t("infoMessage")}
                  </GridItem>
                </Grid>
              )}
            </DataListContent>
          </DataListItem>
        ))}
      </DataList>
    </Page>
  );
};

export default Applications;
