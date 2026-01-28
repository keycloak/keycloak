import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import {
  Action,
  KeycloakDataTable,
  KeycloakSpinner,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Flex,
  FlexItem,
  FormGroup,
  Label,
  PageSection,
  Radio,
  Title,
  ToolbarItem,
} from "@patternfly/react-core";
import { omit } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import CodeEditor from "../components/form/CodeEditor";
import { useRealm } from "../context/realm-context/RealmContext";
import { prettyPrintJSON } from "../util";
import { toAddClientProfile } from "./routes/AddClientProfile";
import { toClientProfile } from "./routes/ClientProfile";

import "./realm-settings-section.css";

type ClientProfile = ClientProfileRepresentation & {
  global: boolean;
};

export default function ProfilesTab() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
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
        }),
      );

      const profiles = allProfiles.profiles?.map((profiles) => ({
        ...profiles,
        global: false,
      }));

      const allClientProfiles = globalProfiles?.concat(profiles ?? []);
      setTableProfiles(allClientProfiles || []);
      setCode(JSON.stringify(allClientProfiles, null, 2));
    },
    [key],
  );

  const loader = async () => tableProfiles ?? [];

  const normalizeProfile = (
    profile: ClientProfile,
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
          (profile) =>
            profile.name !== selectedProfile?.name && !profile.global,
        )
        .map<ClientProfileRepresentation>((profile) =>
          normalizeProfile(profile),
        );

      try {
        await adminClient.clientPolicies.createProfiles({
          profiles: updatedProfiles,
          globalProfiles,
        });
        addAlert(t("deleteClientSuccess"), AlertVariant.success);
        setKey(key + 1);
      } catch (error) {
        addError("deleteClientError", error);
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
        addAlert(t("updateClientProfilesSuccess"), AlertVariant.success);
        setKey(key + 1);
      } catch (error) {
        addError("updateClientProfilesError", error);
      }
    } catch (error) {
      addError("invalidJsonClientProfilesError", error);
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
              className="kc-form-radio-btn pf-v5-u-mr-sm pf-v5-u-ml-sm"
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
          ariaLabelKey="profiles"
          searchPlaceholderKey="clientProfileSearch"
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
              title: t("delete"),
              onRowClick: (profile) => {
                setSelectedProfile(profile);
                toggleDeleteDialog();
              },
            } as Action<ClientProfile>,
          ]}
          columns={[
            {
              name: "name",
              displayKey: t("name"),
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
          <div className="pf-v5-u-mt-md pf-v5-u-ml-lg">
            <CodeEditor
              value={code}
              language="json"
              onChange={(value) => setCode(value ?? "")}
              height={480}
            />
          </div>
          <ActionGroup>
            <div className="pf-v5-u-mt-md">
              <Button
                variant={ButtonVariant.primary}
                className="pf-v5-u-mr-md pf-v5-u-ml-lg"
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
