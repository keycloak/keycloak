import React, { useMemo, useState } from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { Divider, Flex, FlexItem, Radio, Title } from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../context/auth/AdminClient";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { Link } from "react-router-dom";
import "./RealmSettingsSection.css";
import type ClientPolicyExecutorRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyExecutorRepresentation";
import { toNewClientProfile } from "./routes/NewClientProfile";

type ClientProfile = {
  description?: string;
  executors?: ClientPolicyExecutorRepresentation[];
  name?: string;
  global: boolean;
};

export const ProfilesTab = () => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [profiles, setProfiles] = useState<ClientProfile[]>();
  const [show, setShow] = useState(false);

  const loader = async () => {
    const allProfiles = await adminClient.clientPolicies.listProfiles({
      realm,
      includeGlobalProfiles: true,
    });

    const globalProfiles = allProfiles.globalProfiles?.map(
      (globalProfiles) => ({
        ...globalProfiles,
        global: true,
      })
    );

    const profiles = allProfiles.profiles?.map((profiles) => ({
      ...profiles,
      global: false,
    }));

    const allClientProfiles = globalProfiles?.concat(profiles ?? []);
    setProfiles(allClientProfiles);

    return allClientProfiles ?? [];
  };

  const code = useMemo(() => JSON.stringify(profiles, null, 2), [profiles]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientProfileConfirmTitle"),
    messageKey: t("deleteClientProfileConfirm"),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        // delete client profile here
        addAlert(t("deleteClientSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("deleteClientError"), error);
      }
    },
  });

  const cellFormatter = (row: ClientProfile) => (
    <Link to={""} key={row.name}>
      {row.name} {""}
      {row.global && <Label color="blue">{t("global")}</Label>}
    </Link>
  );

  return (
    <>
      <DeleteConfirm />
      <PageSection>
        <Flex className="kc-profiles-config-section">
          <FlexItem>
            <Title headingLevel="h1" size="md">
              {t("profilesConfigType")}
            </Title>
          </FlexItem>
          <FlexItem>
            <Radio
              isChecked={!show}
              name="formView"
              onChange={() => setShow(false)}
              label={t("profilesConfigTypes.formView")}
              id="formView-radioBtn"
              className="kc-form-radio-btn pf-u-mr-sm pf-u-ml-sm"
            />
          </FlexItem>
          <FlexItem>
            <Radio
              isChecked={show}
              name="jsonEditor"
              onChange={() => setShow(true)}
              label={t("profilesConfigTypes.jsonEditor")}
              id="jsonEditor-radioBtn"
              className="kc-editor-radio-btn"
            />
          </FlexItem>
        </Flex>
      </PageSection>
      <Divider />
      {!show ? (
        <KeycloakDataTable
          ariaLabelKey="userEventsRegistered"
          searchPlaceholderKey="realm-settings:clientProfileSearch"
          isPaginated
          loader={loader}
          toolbarItem={
            <ToolbarItem>
              <Button
                id="createProfile"
                component={Link}
                // @ts-ignore
                to={toNewClientProfile({ realm })}
                data-testid="createProfile"
              >
                {t("createClientProfile")}
              </Button>
            </ToolbarItem>
          }
          isRowDisabled={(value) => value.global === true}
          actions={[
            {
              title: t("common:delete"),
              onRowClick: () => {
                toggleDeleteDialog();
              },
            },
          ]}
          columns={[
            {
              name: "name",
              displayKey: t("clientProfileName"),
              cellRenderer: cellFormatter,
            },
            {
              name: "description",
              displayKey: t("clientProfileDescription"),
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyClientProfiles")}
              instructions={t("emptyClientProfilesInstructions")}
            />
          }
        />
      ) : (
        <>
          <div className="pf-u-mt-md pf-u-ml-lg">
            <CodeEditor
              isLineNumbersVisible
              isLanguageLabelVisible
              code={code}
              language={Language.json}
              height="30rem"
            />
          </div>
          <div className="pf-u-mt-md">
            <Button
              variant={ButtonVariant.primary}
              className="pf-u-mr-md pf-u-ml-lg"
            >
              {t("save")}
            </Button>
            <Button variant={ButtonVariant.secondary}> {t("reload")}</Button>
          </div>
        </>
      )}
    </>
  );
};
