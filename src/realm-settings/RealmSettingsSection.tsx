import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import { useErrorHandler } from "react-error-boundary";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  ClipboardCopy,
  DropdownItem,
  DropdownSeparator,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Stack,
  StackItem,
  Switch,
  Tab,
  TabTitleText,
  TextInput,
} from "@patternfly/react-core";

import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { getBaseUrl, toUpperCase } from "../util";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAdminClient, asyncStateFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormattedLink } from "../components/external-link/FormattedLink";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { PartialImportDialog } from "./PartialImport";

type RealmSettingsHeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
  save: () => void;
  realmName: string;
};

const RealmSettingsHeader = ({
  save,
  onChange,
  value,
  realmName,
}: RealmSettingsHeaderProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { setRealm } = useRealm();
  const [partialImportOpen, setPartialImportOpen] = useState(false);

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "realm-settings:disableConfirmTitle",
    messageKey: "realm-settings:disableConfirm",
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "realm-settings:deleteConfirmTitle",
    messageKey: "realm-settings:deleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.realms.del({ realm: realmName });
        addAlert(t("deletedSuccess"), AlertVariant.success);
        setRealm("master");
        history.push("/master/");
      } catch (error) {
        addAlert(t("deleteError", { error }), AlertVariant.danger);
      }
    },
  });

  return (
    <>
      <DisableConfirm />
      <DeleteConfirm />
      <PartialImportDialog
        open={partialImportOpen}
        toggleDialog={() => setPartialImportOpen(!partialImportOpen)}
      />
      <ViewHeader
        titleKey={toUpperCase(realmName)}
        subKey=""
        divider={false}
        dropdownItems={[
          <DropdownItem
            key="import"
            data-testid="openPartialImportModal"
            onClick={() => {
              setPartialImportOpen(true);
            }}
          >
            {t("partialImport")}
          </DropdownItem>,
          <DropdownItem key="export" onClick={() => {}}>
            {t("partialExport")}
          </DropdownItem>,
          <DropdownSeparator key="separator" />,
          <DropdownItem key="delete" onClick={toggleDeleteDialog}>
            {t("common:delete")}
          </DropdownItem>,
        ]}
        isEnabled={value}
        onToggle={(value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            onChange(value);
            save();
          }
        }}
      />
    </>
  );
};

const requireSslTypes = ["all", "external", "none"];

export const RealmSettingsSection = () => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const handleError = useErrorHandler();
  const { realm: realmName } = useRealm();
  const { addAlert } = useAlerts();
  const { register, control, getValues, setValue, handleSubmit } = useForm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [open, setOpen] = useState(false);

  const baseUrl = getBaseUrl(adminClient);

  useEffect(() => {
    return asyncStateFetch(
      () => adminClient.realms.findOne({ realm: realmName }),
      (realm) => {
        setRealm(realm);
        setupForm(realm);
      },
      handleError
    );
  }, []);

  const setupForm = (realm: RealmRepresentation) => {
    Object.entries(realm).map((entry) => setValue(entry[0], entry[1]));
  };

  const save = async (realm: RealmRepresentation) => {
    try {
      await adminClient.realms.update({ realm: realmName }, realm);
      setRealm(realm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(t("saveError", { error }), AlertVariant.danger);
    }
  };

  return (
    <>
      <Controller
        name="enabled"
        control={control}
        defaultValue={true}
        render={({ onChange, value }) => (
          <RealmSettingsHeader
            value={value}
            onChange={onChange}
            realmName={realmName}
            save={() => save(getValues())}
          />
        )}
      />

      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakTabs isBox>
          <Tab
            eventKey="general"
            title={<TabTitleText>{t("general")}</TabTitleText>}
          >
            <PageSection variant="light">
              <FormAccess
                isHorizontal
                role="manage-realm"
                className="pf-u-mt-lg"
                onSubmit={handleSubmit(save)}
              >
                <FormGroup
                  label={t("realmId")}
                  fieldId="kc-realm-id"
                  isRequired
                >
                  <ClipboardCopy isReadOnly>{realmName}</ClipboardCopy>
                </FormGroup>
                <FormGroup label={t("displayName")} fieldId="kc-display-name">
                  <TextInput
                    type="text"
                    id="kc-display-name"
                    name="displayName"
                    ref={register}
                  />
                </FormGroup>
                <FormGroup
                  label={t("htmlDisplayName")}
                  fieldId="kc-html-display-name"
                >
                  <TextInput
                    type="text"
                    id="kc-html-display-name"
                    name="displayNameHtml"
                    ref={register}
                  />
                </FormGroup>
                <FormGroup
                  label={t("frontendUrl")}
                  fieldId="kc-frontend-url"
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:frontendUrl"
                      forLabel={t("frontendUrl")}
                      forID="kc-frontend-url"
                    />
                  }
                >
                  <TextInput
                    type="text"
                    id="kc-frontend-url"
                    name="attributes.frontendUrl"
                    ref={register}
                  />
                </FormGroup>
                <FormGroup
                  label={t("requireSsl")}
                  fieldId="kc-require-ssl"
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:requireSsl"
                      forLabel={t("requireSsl")}
                      forID="kc-require-ssl"
                    />
                  }
                >
                  <Controller
                    name="sslRequired"
                    defaultValue="none"
                    control={control}
                    render={({ onChange, value }) => (
                      <Select
                        toggleId="kc-require-ssl"
                        onToggle={() => setOpen(!open)}
                        onSelect={(_, value) => {
                          onChange(value as string);
                          setOpen(false);
                        }}
                        selections={value}
                        variant={SelectVariant.single}
                        aria-label={t("requireSsl")}
                        isOpen={open}
                      >
                        {requireSslTypes.map((sslType) => (
                          <SelectOption
                            selected={sslType === value}
                            key={sslType}
                            value={sslType}
                          >
                            {t(`sslType.${sslType}`)}
                          </SelectOption>
                        ))}
                      </Select>
                    )}
                  />
                </FormGroup>
                <FormGroup
                  hasNoPaddingTop
                  label={t("userManagedAccess")}
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:userManagedAccess"
                      forLabel={t("userManagedAccess")}
                      forID="kc-user-manged-access"
                    />
                  }
                  fieldId="kc-user-manged-access"
                >
                  <Controller
                    name="userManagedAccessAllowed"
                    control={control}
                    defaultValue={false}
                    render={({ onChange, value }) => (
                      <Switch
                        id="kc-user-manged-access"
                        label={t("common:on")}
                        labelOff={t("common:off")}
                        isChecked={value}
                        onChange={onChange}
                      />
                    )}
                  />
                </FormGroup>
                <FormGroup
                  label={t("endpoints")}
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:endpoints"
                      forLabel={t("endpoints")}
                      forID="kc-endpoints"
                    />
                  }
                  fieldId="kc-endpoints"
                >
                  <Stack>
                    <StackItem>
                      <FormattedLink
                        href={`${baseUrl}realms/${realmName}/.well-known/openid-configuration`}
                        title={t("openEndpointConfiguration")}
                      />
                    </StackItem>
                    <StackItem>
                      <FormattedLink
                        href={`${baseUrl}realms/${realmName}/protocol/saml/descriptor`}
                        title={t("samlIdentityProviderMetadata")}
                      />
                    </StackItem>
                  </Stack>
                </FormGroup>

                <ActionGroup>
                  <Button variant="primary" type="submit">
                    {t("common:save")}
                  </Button>
                  <Button variant="link" onClick={() => setupForm(realm!)}>
                    {t("common:revert")}
                  </Button>
                </ActionGroup>
              </FormAccess>
            </PageSection>
          </Tab>
        </KeycloakTabs>
      </PageSection>
    </>
  );
};
