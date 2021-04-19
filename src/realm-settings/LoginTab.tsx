import React, { useEffect, useState } from "react";
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
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { useErrorHandler } from "react-error-boundary";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";

export const RealmSettingsLoginTab = () => {
  const { t } = useTranslation("realm-settings");
  const [realm, setRealm] = useState<RealmRepresentation>();
  const handleError = useErrorHandler();
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert } = useAlerts();

  useEffect(() => {
    return asyncStateFetch(
      () => adminClient.realms.findOne({ realm: realmName }),
      (realm) => {
        setRealm(realm);
      },
      handleError
    );
  }, []);

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
      <PageSection variant="light">
        <FormPanel title="Login screen customization">
          {
            <FormAccess isHorizontal role="manage-realm">
              <FormGroup
                label={t("userRegistration")}
                fieldId="kc-user-reg"
                labelIcon={
                  <HelpItem
                    helpText={t("userRegistrationHelpText")}
                    forLabel={t("userRegistration")}
                    forID="kc-user-reg"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-user-reg"
                  data-testid="user-reg-switch"
                  name="registrationAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.registrationAllowed}
                  onChange={(value) => {
                    save({ ...realm, registrationAllowed: value });
                  }}
                />
              </FormGroup>
              <FormGroup
                label={t("forgotPassword")}
                fieldId="kc-forgot-pw"
                labelIcon={
                  <HelpItem
                    helpText={t("forgotPasswordHelpText")}
                    forLabel={t("forgotPassword")}
                    forID="kc-forgot-pw"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-forgot-pw"
                  data-testid="forgot-pw-switch"
                  name="resetPasswordAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.resetPasswordAllowed}
                  onChange={(value) => {
                    save({ ...realm, resetPasswordAllowed: value });
                  }}
                />
              </FormGroup>
              <FormGroup
                label={t("rememberMe")}
                fieldId="kc-remember-me"
                labelIcon={
                  <HelpItem
                    helpText={t("rememberMeHelpText")}
                    forLabel={t("rememberMe")}
                    forID="kc-remember-me"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-remember-me"
                  data-testid="remember-me-switch"
                  name="rememberMe"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.rememberMe}
                  onChange={(value) => {
                    save({ ...realm, rememberMe: value });
                  }}
                />
              </FormGroup>
            </FormAccess>
          }
        </FormPanel>
        <FormPanel title="Email settings">
          {
            <FormAccess isHorizontal role="manage-realm">
              <FormGroup
                label={t("emailAsUsername")}
                fieldId="kc-email-as-username"
                labelIcon={
                  <HelpItem
                    helpText={t("emailAsUsernameHelpText")}
                    forLabel={t("emailAsUsername")}
                    forID="kc-email-as-username"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-email-as-username"
                  data-testid="email-as-username-switch"
                  name="registrationEmailAsUsername"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.registrationEmailAsUsername}
                  onChange={(value) => {
                    save({ ...realm, registrationEmailAsUsername: value });
                  }}
                />
              </FormGroup>
              <FormGroup
                label={t("loginWithEmail")}
                fieldId="kc-login-with-email"
                labelIcon={
                  <HelpItem
                    helpText={t("loginWithEmailHelpText")}
                    forLabel={t("loginWithEmail")}
                    forID="kc-login-with-email"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-login-with-email"
                  data-testid="login-with-email-switch"
                  name="loginWithEmailAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.loginWithEmailAllowed}
                  onChange={(value) => {
                    save({ ...realm, loginWithEmailAllowed: value });
                  }}
                />
              </FormGroup>
              <FormGroup
                label={t("duplicateEmails")}
                fieldId="kc-duplicate-emails"
                labelIcon={
                  <HelpItem
                    helpText={t("duplicateEmailsHelpText")}
                    forLabel={t("duplicateEmails")}
                    forID="kc-duplicate-emails"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-duplicate-emails"
                  data-testid="duplicate-emails-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  name="duplicateEmailsAllowed"
                  isChecked={
                    realm?.duplicateEmailsAllowed &&
                    !realm?.loginWithEmailAllowed &&
                    !realm?.registrationEmailAsUsername
                  }
                  onChange={(value) => {
                    save({ ...realm, duplicateEmailsAllowed: value });
                  }}
                  isDisabled={
                    realm?.loginWithEmailAllowed ||
                    realm?.registrationEmailAsUsername
                  }
                />
              </FormGroup>
              <FormGroup
                label={t("verifyEmail")}
                fieldId="kc-verify-email"
                labelIcon={
                  <HelpItem
                    helpText={t("verifyEmailHelpText")}
                    forLabel={t("verifyEmail")}
                    forID="kc-verify-email"
                  />
                }
                hasNoPaddingTop
              >
                <Switch
                  id="kc-verify-email"
                  data-testid="verify-email-switch"
                  name="verifyEmail"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={realm?.verifyEmail}
                  onChange={(value) => {
                    save({ ...realm, verifyEmail: value });
                  }}
                />
              </FormGroup>
            </FormAccess>
          }
        </FormPanel>
      </PageSection>
    </>
  );
};
