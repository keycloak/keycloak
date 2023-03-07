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
import { TFuncKey } from "i18next";
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
import useFormatter from "../components/format/format-date";
import { Page } from "../components/page/Page";
import { keycloak } from "../keycloak";
import { usePromise } from "../utils/usePromise";

type MobileLinkProps = {
  title: string;
  onClick: () => void;
};

const MobileLink = ({ title, onClick }: MobileLinkProps) => {
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
      >
        {title}
      </Button>
    </>
  );
};

const SigningIn = () => {
  const { t } = useTranslation();
  const { formatDate } = useFormatter();
  const { addAlert, addError } = useAlerts();
  const { login } = keycloak;

  const [credentials, setCredentials] = useState<CredentialContainer[]>();
  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise((signal) => getCredentials({ signal }), setCredentials, [key]);

  const credentialRowCells = (
    credMetadata: CredentialMetadataRepresentation
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
        </DataListCell>
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
      <DataList aria-label="user credential" className="pf-u-mb-xl">
        {credentials.map((container) => (
          <PageSection
            key={container.category}
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
                      title={t("setUpNew", [
                        t(container.displayName as TFuncKey),
                      ])}
                    />
                  </div>
                </SplitItem>
              )}
            </Split>

            <DataList aria-label="credential list" className="pf-u-mb-xl">
              {container.userCredentialMetadatas.length === 0 && (
                <EmptyRow
                  message={t("notSetUp", [
                    t(container.displayName as TFuncKey),
                  ])}
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
                              buttonTitle="remove"
                              buttonVariant="danger"
                              modalTitle={t("removeCred", [
                                label(meta.credential),
                              ])}
                              modalMessage={t("stopUsingCred", [
                                label(meta.credential),
                              ])}
                              onContinue={async () => {
                                try {
                                  await deleteCredentials(meta.credential);
                                  addAlert(
                                    t("successRemovedMessage", {
                                      userLabel: label(meta.credential),
                                    })
                                  );
                                  refresh();
                                } catch (error) {
                                  addError(
                                    t("errorRemovedMessage", {
                                      userLabel: label(meta.credential),
                                      error,
                                    }).toString()
                                  );
                                }
                              }}
                            />
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
      </DataList>
    </Page>
  );
};

export default SigningIn;
