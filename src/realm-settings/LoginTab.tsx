import React from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Controller, useForm } from "react-hook-form";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";

type RealmSettingsLoginTabProps = {
  realm: RealmRepresentation;
  refresh: () => void;
};

export const RealmSettingsLoginTab = ({
  realm,
  refresh,
}: RealmSettingsLoginTabProps) => {
  const { t } = useTranslation("realm-settings");

  const form = useForm<RealmRepresentation>({ mode: "onChange" });
  const { addAlert, addError } = useAlerts();
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();

  const updateSwitchValue = async (
    onChange: (newValue: boolean) => void,
    value: boolean
  ) => {
    onChange(value);
    const switchValues = form.getValues();

    try {
      await adminClient.realms.update(
        {
          realm: realmName,
        },
        switchValues
      );
      addAlert(t("deleteClientPolicySuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError(t("deleteClientPolicyError"), error);
    }
  };

  return (
    <PageSection variant="light">
      <FormPanel
        className="kc-login-screen"
        title={t("loginScreenCustomization")}
      >
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("userRegistration")}
            fieldId="kc-user-reg"
            labelIcon={
              <HelpItem
                helpText={t("userRegistrationHelpText")}
                fieldLabelId="realm-settings:userRegistration"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="registrationAllowed"
              defaultValue={realm.registrationAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-user-reg-switch"
                  data-testid="user-reg-switch"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="kc-forgot-pw"
            labelIcon={
              <HelpItem
                helpText="realm-settings:forgotPasswordHelpText"
                fieldLabelId="realm-settings:forgotPassword"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="resetPasswordAllowed"
              defaultValue={realm.resetPasswordAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-forgot-pw-switch"
                  data-testid="forgot-pw-switch"
                  name="resetPasswordAllowed"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("rememberMe")}
            fieldId="kc-remember-me"
            labelIcon={
              <HelpItem
                helpText="realm-settings:rememberMeHelpText"
                fieldLabelId="realm-settings:rememberMe"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="rememberMe"
              defaultValue={realm.rememberMe}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-remember-me-switch"
                  data-testid="remember-me-switch"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel className="kc-email-settings" title={t("emailSettings")}>
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("emailAsUsername")}
            fieldId="kc-email-as-username"
            labelIcon={
              <HelpItem
                helpText="realm-settings:emailAsUsernameHelpText"
                fieldLabelId="realm-settings:emailAsUsername"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="registrationEmailAsUsername"
              defaultValue={realm.registrationEmailAsUsername}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-email-as-username-switch"
                  data-testid="email-as-username-switch"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("loginWithEmail")}
            fieldId="kc-login-with-email"
            labelIcon={
              <HelpItem
                helpText="realm-settings:loginWithEmailHelpText"
                fieldLabelId="realm-settings:loginWithEmail"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="loginWithEmailAllowed"
              defaultValue={realm.loginWithEmailAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-login-with-email-switch"
                  data-testid="login-with-email-switch"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("duplicateEmails")}
            fieldId="kc-duplicate-emails"
            labelIcon={
              <HelpItem
                helpText="realm-settings:duplicateEmailsHelpText"
                fieldLabelId="realm-settings:duplicateEmails"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="duplicateEmailsAllowed"
              defaultValue={realm.duplicateEmailsAllowed}
              control={form.control}
              render={({ onChange }) => (
                <Switch
                  id="kc-duplicate-emails-switch"
                  data-testid="duplicate-emails-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={
                    form.getValues().duplicateEmailsAllowed &&
                    !form.getValues().loginWithEmailAllowed &&
                    !form.getValues().registrationEmailAsUsername
                  }
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                  isDisabled={
                    form.getValues().loginWithEmailAllowed ||
                    form.getValues().registrationEmailAsUsername
                  }
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("verifyEmail")}
            fieldId="kc-verify-email"
            labelIcon={
              <HelpItem
                helpText="realm-settings:verifyEmailHelpText"
                fieldLabelId="realm-settings:verifyEmail"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="verifyEmail"
              defaultValue={realm.verifyEmail}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-verify-email-switch"
                  data-testid="verify-email-switch"
                  name="verifyEmail"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        className="kc-user-info-settings"
        title={t("userInfoSettings")}
      >
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("editUsername")}
            fieldId="kc-edit-username"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:editUsername"
                fieldLabelId="realm-settings:editUsername"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="editUsernameAllowed"
              defaultValue={realm.editUsernameAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-edit-username-switch"
                  data-testid="edit-username-switch"
                  value={value ? "on" : "off"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value);
                  }}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
