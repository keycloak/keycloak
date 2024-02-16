import {
  Button,
  DataList,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Dropdown,
  DropdownItem,
  KebabToggle,
  PageSection,
  Spinner,
  Split,
  SplitItem,
  Title,
} from "@patternfly/react-core";
import { CSSProperties, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { ContinueCancelModal, useAlerts } from "ui-shared";
import { deleteCredentials, getCredentials } from "../api/methods";
import {
  CredentialContainer,
  CredentialMetadataRepresentation,
  CredentialRepresentation,
} from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { Page } from "../components/page/Page";
import { TFuncKey } from "../i18n";
import { formatDate } from "../utils/formatDate";
import { usePromise } from "../utils/usePromise";
import { useEnvironment } from "../root/KeycloakContext";

type MobileLinkProps = {
  title: string;
  onClick: () => void;
  testid?: string;
};

const MobileLink = ({ title, onClick, testid }: MobileLinkProps) => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Dropdown
        isPlain
        position="right"
        toggle={<KebabToggle onToggle={setOpen} />}
        className="pf-u-display-none-on-lg"
        isOpen={open}
        dropdownItems={[
          <DropdownItem key="1" onClick={onClick}>
            {title}
          </DropdownItem>,
        ]}
      />
      <Button
        variant="link"
        onClick={onClick}
        className="pf-u-display-none pf-u-display-inline-flex-on-lg"
        data-testid={testid}
      >
        {title}
      </Button>
    </>
  );
};

export const SigningIn = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAlerts();
  const { login } = context.keycloak;

  const [credentials, setCredentials] = useState<CredentialContainer[]>();
  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise((signal) => getCredentials({ signal, context }), setCredentials, [
    key,
  ]);

  const credentialRowCells = (
    credMetadata: CredentialMetadataRepresentation,
  ) => {
    const credential = credMetadata.credential;
    const maxWidth = { "--pf-u-max-width--MaxWidth": "300px" } as CSSProperties;
    const items = [
      <DataListCell
        id={`cred-${credMetadata.credential.id}`}
        key="title"
        className="pf-u-max-width"
        style={maxWidth}
      >
        {credential.userLabel || t(credential.type as TFuncKey)}
      </DataListCell>,
    ];

    if (credential.createdDate) {
      items.push(
        <DataListCell key={"created" + credential.id}>
          <Trans i18nKey="credentialCreatedAt">
            <strong className="pf-u-mr-md"></strong>
            {{ date: formatDate(new Date(credential.createdDate)) }}
          </Trans>
        </DataListCell>,
      );
    }
    return items;
  };

  const label = (credential: CredentialRepresentation) =>
    credential.userLabel || t(credential.type as TFuncKey);

  if (!credentials) {
    return <Spinner />;
  }

  return (
    <Page title={t("signingIn")} description={t("signingInDescription")}>
      {credentials.map((container) => (
        <PageSection
          key={container.category + container.type}
          variant="light"
          className="pf-u-px-0"
        >
          <Title headingLevel="h2" size="xl">
            {t(container.category as TFuncKey)}
          </Title>
          <Split className="pf-u-mt-lg pf-u-mb-lg">
            <SplitItem>
              <Title headingLevel="h3" size="md" className="pf-u-mb-md">
                <span className="cred-title pf-u-display-block">
                  {t(container.displayName as TFuncKey)}
                </span>
              </Title>
              {t(container.helptext as TFuncKey)}
            </SplitItem>
            {container.createAction && (
              <SplitItem isFilled>
                <div className="pf-u-float-right">
                  <MobileLink
                    onClick={() =>
                      login({
                        action: container.createAction,
                      })
                    }
                    title={t("setUpNew", {
                      name: t(container.displayName as TFuncKey),
                    })}
                    testid={`${container.category}/create`}
                  />
                </div>
              </SplitItem>
            )}
          </Split>

          <DataList
            aria-label="credential list"
            className="pf-u-mb-xl"
            data-testid={`${container.category}/credential-list`}
          >
            {container.userCredentialMetadatas.length === 0 && (
              <EmptyRow
                message={t("notSetUp", {
                  name: t(container.displayName as TFuncKey),
                })}
              />
            )}

            {container.userCredentialMetadatas.map((meta) => (
              <DataListItem key={meta.credential.id}>
                <DataListItemRow>
                  <DataListItemCells
                    className="pf-u-py-0"
                    dataListCells={[
                      ...credentialRowCells(meta),
                      <DataListAction
                        key="action"
                        id={`action-${meta.credential.id}`}
                        aria-label={t("updateCredAriaLabel")}
                        aria-labelledby={`cred-${meta.credential.id}`}
                      >
                        {container.removeable ? (
                          <ContinueCancelModal
                            buttonTitle={t("delete")}
                            modalTitle={t("removeCred", {
                              name: label(meta.credential),
                            })}
                            continueLabel={t("confirm")}
                            cancelLabel={t("cancel")}
                            buttonVariant="danger"
                            onContinue={async () => {
                              try {
                                await deleteCredentials(
                                  context,
                                  meta.credential,
                                );
                                addAlert(
                                  t("successRemovedMessage", {
                                    userLabel: label(meta.credential),
                                  }),
                                );
                                refresh();
                              } catch (error) {
                                addError(
                                  t("errorRemovedMessage", {
                                    userLabel: label(meta.credential),
                                    error,
                                  }).toString(),
                                );
                              }
                            }}
                          >
                            {t("stopUsingCred", {
                              name: label(meta.credential),
                            })}
                          </ContinueCancelModal>
                        ) : (
                          <Button
                            variant="secondary"
                            onClick={() => {
                              if (container.updateAction)
                                login({ action: container.updateAction });
                            }}
                          >
                            {t("update")}
                          </Button>
                        )}
                      </DataListAction>,
                    ]}
                  />
                </DataListItemRow>
              </DataListItem>
            ))}
          </DataList>
        </PageSection>
      ))}
    </Page>
  );
};

export default SigningIn;
