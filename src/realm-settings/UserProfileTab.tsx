import type ClientProfilesRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfilesRepresentation";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import type { editor } from "monaco-editor";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { prettyPrintJSON } from "../util";

export const UserProfileTab = () => {
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const [activeTab, setActiveTab] = useState("attributes");
  const [profiles, setProfiles] = useState<ClientProfilesRepresentation>();
  const [isSaving, setIsSaving] = useState(false);
  const [refreshCount, setRefreshCount] = useState(0);

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({
        includeGlobalProfiles: true,
        realm,
      }),
    (profiles) => setProfiles(profiles),
    [refreshCount]
  );

  async function onSave(updatedProfiles: ClientProfilesRepresentation) {
    setIsSaving(true);

    try {
      await adminClient.clientPolicies.createProfiles({
        ...updatedProfiles,
        realm,
      });

      setRefreshCount(refreshCount + 1);
      addAlert(t("userProfileSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:userProfileError", error);
    }

    setIsSaving(false);
  }

  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key.toString())}
      mountOnEnter
    >
      <Tab
        eventKey="attributes"
        title={<TabTitleText>{t("attributes")}</TabTitleText>}
      ></Tab>
      <Tab
        eventKey="attributesGroup"
        title={<TabTitleText>{t("attributesGroup")}</TabTitleText>}
      ></Tab>
      <Tab
        eventKey="jsonEditor"
        title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
      >
        <JsonEditorTab
          profiles={profiles}
          onSave={onSave}
          isSaving={isSaving}
        />
      </Tab>
    </Tabs>
  );
};

type JsonEditorTabProps = {
  profiles?: ClientProfilesRepresentation;
  onSave: (profiles: ClientProfilesRepresentation) => void;
  isSaving: boolean;
};

const JsonEditorTab = ({ profiles, onSave, isSaving }: JsonEditorTabProps) => {
  const { t } = useTranslation();
  const { addError } = useAlerts();
  const [editor, setEditor] = useState<editor.IStandaloneCodeEditor>();

  useEffect(() => resetCode(), [profiles, editor]);

  function resetCode() {
    editor?.setValue(profiles ? prettyPrintJSON(profiles) : "");
  }

  function save() {
    const value = editor?.getValue();

    if (!value) {
      return;
    }

    try {
      onSave(JSON.parse(value));
    } catch (error) {
      addError("realm-settings:invalidJsonError", error);
      return;
    }
  }

  return (
    <PageSection variant="light">
      <CodeEditor
        language={Language.json}
        height="30rem"
        onEditorDidMount={(editor) => setEditor(editor)}
        isLanguageLabelVisible
      />
      <Form>
        <ActionGroup>
          <Button variant="primary" onClick={save} isDisabled={isSaving}>
            {t("common:save")}
          </Button>
          <Button variant="link" onClick={resetCode} isDisabled={isSaving}>
            {t("common:revert")}
          </Button>
        </ActionGroup>
      </Form>
    </PageSection>
  );
};
