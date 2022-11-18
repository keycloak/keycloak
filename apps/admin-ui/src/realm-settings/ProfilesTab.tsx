import { useState } from "react";
import { omit } from "lodash-es";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  Label,
  PageSection,
  ToolbarItem,
  Divider,
  Flex,
  FlexItem,
  Radio,
  Title,
} from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useTranslation } from "react-i18next";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { prettyPrintJSON } from "../util";
import { Link } from "react-router-dom-v5-compat";
import { toAddClientProfile } from "./routes/AddClientProfile";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import { toClientProfile } from "./routes/ClientProfile";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";

import "./realm-settings-section.css";

type ClientProfile = ClientProfileRepresentation & {
  global: boolean;
};

export default function ProfilesTab() {
  const { t } = useTranslation("realm-settings");
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [tableProfiles, setTableProfiles] = useState<ClientProfile[]>();
  const [globalProfiles, setGlobalProfiles] =
    useState<ClientProfileRepresentation[]>();
  const [selectedProfile, setSelectedProfile] = useState<ClientProfile>();
  const [show, setShow] = useState(false);
  const [code, setCode] = useState<string>();
  const [key, setKey] = useState(0);

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({
        includeGlobalProfiles: true,
      }),
    (allProfiles) => {
      setGlobalProfiles(allProfiles.globalProfiles);

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
      setTableProfiles(allClientProfiles || []);
      setCode(JSON.stringify(allClientProfiles, null, 2));
    },
    [key]
  );

  const loader = async () => tableProfiles ?? [];

  const normalizeProfile = (
    profile: ClientProfile
  ): ClientProfileRepresentation => omit(profile, "global");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientProfileConfirmTitle"),
    messageKey: t("deleteClientProfileConfirm", {
      profileName: selectedProfile?.name,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedProfiles = tableProfiles
        ?.filter(
          (profile) => profile.name !== selectedProfile?.name && !profile.global
        )
        .map<ClientProfileRepresentation>((profile) =>
          normalizeProfile(profile)
        );

      try {
        await adminClient.clientPolicies.createProfiles({
          profiles: updatedProfiles,
          globalProfiles,
        });
        addAlert(t("deleteClientSuccess"), AlertVariant.success);
        setKey(key + 1);
      } catch (error) {
        addError(t("deleteClientError"), error);
      }
    },
  });

  const cellFormatter = (row: ClientProfile) => (
    <Link
      to={toClientProfile({
        realm,
        profileName: row.name!,
      })}
      key={row.name}
    >
      {row.name} {row.global && <Label color="blue">{t("global")}</Label>}
    </Link>
  );

  if (!tableProfiles) {
    return <KeycloakSpinner />;
  }

  const save = async () => {
    if (!code) {
      return;
    }

    try {
      const obj: ClientProfile[] = JSON.parse(code);
      const changedProfiles = obj
        .filter((profile) => !profile.global)
        .map((profile) => normalizeProfile(profile));

      const changedGlobalProfiles = obj
        .filter((profile) => profile.global)
        .map((profile) => normalizeProfile(profile));

      try {
        await adminClient.clientPolicies.createProfiles({
          profiles: changedProfiles,
          globalProfiles: changedGlobalProfiles,
        });
        addAlert(
          t("realm-settings:updateClientProfilesSuccess"),
          AlertVariant.success
        );
        setKey(key + 1);
      } catch (error) {
        addError("realm-settings:updateClientProfilesError", error);
      }
    } catch (error) {
      console.warn("Invalid json, ignoring value using {}");
    }
  };

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
              name="profilesView"
              onChange={() => setShow(false)}
              label={t("profilesConfigTypes.formView")}
              id="formView-profilesView"
              className="kc-form-radio-btn pf-u-mr-sm pf-u-ml-sm"
              data-testid="formView-profilesView"
            />
          </FlexItem>
          <FlexItem>
            <Radio
              isChecked={show}
              name="profilesView"
              onChange={() => setShow(true)}
              label={t("profilesConfigTypes.jsonEditor")}
              id="jsonEditor-profilesView"
              className="kc-editor-radio-btn"
              data-testid="jsonEditor-profilesView"
            />
          </FlexItem>
        </Flex>
      </PageSection>
      <Divider />
      {!show ? (
        <KeycloakDataTable
          key={tableProfiles.length}
          ariaLabelKey="realm-settings:profiles"
          searchPlaceholderKey="realm-settings:clientProfileSearch"
          loader={loader}
          toolbarItem={
            <ToolbarItem>
              <Button
                id="createProfile"
                component={(props) => (
                  <Link
                    {...props}
                    to={toAddClientProfile({ realm, tab: "profiles" })}
                  />
                )}
                data-testid="createProfile"
              >
                {t("createClientProfile")}
              </Button>
            </ToolbarItem>
          }
          isRowDisabled={(value) => value.global}
          actions={[
            {
              title: t("common:delete"),
              onRowClick: (profile) => {
                setSelectedProfile(profile);
                toggleDeleteDialog();
              },
            },
          ]}
          columns={[
            {
              name: "name",
              displayKey: t("common:name"),
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
        <FormGroup fieldId={"jsonEditor"}>
          <div className="pf-u-mt-md pf-u-ml-lg">
            <CodeEditor
              isLineNumbersVisible
              isLanguageLabelVisible
              isReadOnly={false}
              code={code}
              language={Language.json}
              height="30rem"
              onChange={(value) => {
                setCode(value ?? "");
              }}
            />
          </div>
          <ActionGroup>
            <div className="pf-u-mt-md">
              <Button
                variant={ButtonVariant.primary}
                className="pf-u-mr-md pf-u-ml-lg"
                onClick={save}
                data-testid="jsonEditor-saveBtn"
              >
                {t("save")}
              </Button>
              <Button
                variant={ButtonVariant.link}
                onClick={() => {
                  setCode(prettyPrintJSON(tableProfiles));
                }}
                data-testid="jsonEditor-reloadBtn"
              >
                {t("reload")}
              </Button>
            </div>
          </ActionGroup>
        </FormGroup>
      )}
    </>
  );
}
