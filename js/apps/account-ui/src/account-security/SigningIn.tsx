import {
  Button,
  DataList,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Dropdown,
  DropdownItem,
  MenuToggle,
  PageSection,
  Spinner,
  Split,
  SplitItem,
  Title,
} from "@patternfly/react-core";
import {
  EllipsisVIcon,
  ExclamationTriangleIcon,
  InfoAltIcon,
} from "@patternfly/react-icons";
import { CSSProperties, Fragment, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { getCredentials } from "../api/methods";
import {
  CredentialContainer,
  CredentialMetadataRepresentation,
} from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { Page } from "../components/page/Page";
import { TFuncKey } from "../i18n";
import { formatDate } from "../utils/formatDate";
import { usePromise } from "../utils/usePromise";

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
        popperProps={{
          position: "right",
        }}
        onOpenChange={(isOpen) => setOpen(isOpen)}
        toggle={(toggleRef) => (
          <MenuToggle
            className="pf-v5-u-display-none-on-lg"
            ref={toggleRef}
            variant="plain"
            onClick={() => setOpen(!open)}
            isExpanded={open}
          >
            <EllipsisVIcon />
          </MenuToggle>
        )}
        isOpen={open}
      >
        <DropdownItem key="1" onClick={onClick}>
          {title}
        </DropdownItem>
      </Dropdown>
      <Button
        variant="link"
        onClick={onClick}
        className="pf-v5-u-display-none pf-v5-u-display-inline-flex-on-lg"
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
  const { login } = context.keycloak;

  const [credentials, setCredentials] = useState<CredentialContainer[]>();

  usePromise(
    (signal) => getCredentials({ signal, context }),
    setCredentials,
    [],
  );

  const credentialRowCells = (
    credMetadata: CredentialMetadataRepresentation,
  ) => {
    const credential = credMetadata.credential;
    const maxWidth = {
      "--pf-v5-u-max-width--MaxWidth": "300px",
    } as CSSProperties;
    const items = [
      <DataListCell
        key="title"
        data-testrole="label"
        className="pf-v5-u-max-width"
        style={maxWidth}
      >
        {t(credential.userLabel) || t(credential.type as TFuncKey)}
      </DataListCell>,
    ];

    if (credential.createdDate) {
      items.push(
        <DataListCell
          key={"created" + credential.id}
          data-testrole="created-at"
        >
          <Trans i18nKey="credentialCreatedAt">
            <strong className="pf-v5-u-mr-md"></strong>
            {{ date: formatDate(new Date(credential.createdDate)) }}
          </Trans>
        </DataListCell>,
      );
    }
    if (
      credMetadata.infoMessage ||
      credMetadata.infoProperties ||
      (credMetadata.warningMessageTitle &&
        credMetadata.warningMessageDescription)
    ) {
      items.push(
        <DataListCell
          key={"warning-message" + credential.id}
          data-testrole="warning-message"
        >
          <>
            {credMetadata.infoMessage && (
              <p>
                <InfoAltIcon />{" "}
                {t(
                  credMetadata.infoMessage.key,
                  credMetadata.infoMessage.parameters?.reduce(
                    (acc, val, idx) => ({ ...acc, [idx]: val }),
                    {},
                  ),
                )}
              </p>
            )}
            {credMetadata.infoProperties && (
              <Split className="pf-v5-u-mb-lg">
                <SplitItem>
                  <InfoAltIcon />
                </SplitItem>
                <SplitItem isFilled className="pf-v5-u-ml-xs">
                  <DescriptionList
                    isHorizontal
                    horizontalTermWidthModifier={{
                      "2xl": "15ch",
                    }}
                  >
                    {credMetadata.infoProperties.map((prop) => (
                      <DescriptionListGroup key={prop.key}>
                        <DescriptionListTerm>{t(prop.key)}</DescriptionListTerm>
                        <DescriptionListDescription>
                          {prop.parameters ? prop.parameters[0] : ""}
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    ))}
                  </DescriptionList>
                </SplitItem>
              </Split>
            )}
            {credMetadata.warningMessageTitle &&
              credMetadata.warningMessageDescription && (
                <>
                  <p>
                    <ExclamationTriangleIcon />{" "}
                    {t(
                      credMetadata.warningMessageTitle.key,
                      credMetadata.warningMessageTitle.parameters?.reduce(
                        (acc, val, idx) => ({ ...acc, [idx]: val }),
                        {},
                      ),
                    )}
                  </p>
                  <p>
                    {t(
                      credMetadata.warningMessageDescription.key,
                      credMetadata.warningMessageDescription.parameters?.reduce(
                        (acc, val, idx) => ({ ...acc, [idx]: val }),
                        {},
                      ),
                    )}
                  </p>
                </>
              )}
          </>
        </DataListCell>,
      );
    }
    return items;
  };

  if (!credentials) {
    return <Spinner />;
  }

  const credentialUniqueCategories = [
    ...new Set(credentials.map((c) => c.category)),
  ];

  return (
    <Page title={t("signingIn")} description={t("signingInDescription")}>
      {credentialUniqueCategories.map((category) => (
        <PageSection key={category} variant="light" className="pf-v5-u-px-0">
          <Title headingLevel="h2" size="xl" id={`${category}-categ-title`}>
            {t(category as TFuncKey)}
          </Title>
          {credentials
            .filter((cred) => cred.category == category)
            .map((container) => (
              <Fragment key={container.type}>
                <Split className="pf-v5-u-mt-lg pf-v5-u-mb-lg">
                  <SplitItem>
                    <Title
                      headingLevel="h3"
                      size="md"
                      className="pf-v5-u-mb-md"
                      data-testid={`${container.type}/help`}
                    >
                      <span
                        className="cred-title pf-v5-u-display-block"
                        data-testid={`${container.type}/title`}
                      >
                        {t(container.displayName as TFuncKey)}
                      </span>
                    </Title>
                    <span data-testid={`${container.type}/help-text`}>
                      {t(container.helptext as TFuncKey)}
                    </span>
                  </SplitItem>
                  {container.createAction && (
                    <SplitItem isFilled>
                      <div className="pf-v5-u-float-right">
                        <MobileLink
                          onClick={() =>
                            login({
                              action: container.createAction,
                            })
                          }
                          title={t("setUpNew", {
                            name: t(
                              `${container.type}-display-name` as TFuncKey,
                            ),
                          })}
                          testid={`${container.type}/create`}
                        />
                      </div>
                    </SplitItem>
                  )}
                </Split>

                <DataList
                  aria-label="credential list"
                  className="pf-v5-u-mb-xl"
                  data-testid={`${container.type}/credential-list`}
                >
                  {container.userCredentialMetadatas.length === 0 && (
                    <EmptyRow
                      message={t("notSetUp", {
                        name: t(container.displayName as TFuncKey),
                      })}
                      data-testid={`${container.type}/not-set-up`}
                    />
                  )}

                  {container.userCredentialMetadatas.map((meta) => (
                    <DataListItem key={meta.credential.id}>
                      <DataListItemRow id={`cred-${meta.credential.id}`}>
                        <DataListItemCells
                          className="pf-v5-u-py-0"
                          dataListCells={[
                            ...credentialRowCells(meta),
                            <DataListAction
                              key="action"
                              id={`action-${meta.credential.id}`}
                              aria-label={t("updateCredAriaLabel")}
                              aria-labelledby={`cred-${meta.credential.id}`}
                            >
                              {container.removeable && (
                                <Button
                                  variant="danger"
                                  data-testrole="remove"
                                  onClick={async () => {
                                    await login({
                                      action:
                                        "delete_credential:" +
                                        meta.credential.id,
                                    });
                                  }}
                                >
                                  {t("delete")}
                                </Button>
                              )}
                              {container.updateAction && (
                                <Button
                                  variant="secondary"
                                  onClick={async () => {
                                    await login({
                                      action: container.updateAction,
                                    });
                                  }}
                                  data-testrole="update"
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
              </Fragment>
            ))}
        </PageSection>
      ))}
    </Page>
  );
};

export default SigningIn;
