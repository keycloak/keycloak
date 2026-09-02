import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormGroup, PageSection, Switch } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { FormPanel, HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { WEBAUTHN_PASSWORDLESS_POLICY } from "../authentication/policies/Policies";
import { toAuthentication } from "../authentication/routes/Authentication";
import { FormAccess } from "../components/form/FormAccess";
import { SettingsShortcut } from "../components/settings-shortcut/SettingsShortcut";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";

type RealmSettingsLoginTabProps = {
  realm: RealmRepresentation;
  refresh: () => void;
};

type SwitchType = { [K in keyof RealmRepresentation]: boolean };

export const RealmSettingsLoginTab = ({
  realm,
  refresh,
}: RealmSettingsLoginTabProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();
  const passkeysVisible = isFeatureEnabled(Feature.Passkeys);
  const [savingFields, setSavingFields] = useState<Set<string>>(new Set());

  const setSaving = (field: string, saving: boolean) => {
    setSavingFields((prev) => {
      const next = new Set(prev);
      if (saving) {
        next.add(field);
      } else {
        next.delete(field);
      }
      return next;
    });
  };

  const updateSwitchValue = async (
    switches: SwitchType | SwitchType[],
    fieldKey?: string,
  ) => {
    const name =
      fieldKey ??
      (Array.isArray(switches)
        ? Object.keys(switches[0])[0]
        : Object.keys(switches)[0]);

    setSaving(name, true);
    try {
      await adminClient.realms.update(
        {
          realm: realmName,
        },
        Array.isArray(switches)
          ? switches.reduce((realm, s) => Object.assign(realm, s), realm)
          : Object.assign(realm, switches),
      );
      addAlert(t("enableSwitchSuccess", { switch: t(name) }));
      refresh();
    } catch (error) {
      addError("enableSwitchError", error);
    } finally {
      setSaving(name, false);
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
            label={t("registrationAllowed")}
            fieldId="kc-user-reg-switch"
            labelIcon={
              <HelpItem
                helpText={t("userRegistrationHelpText")}
                fieldLabelId="registrationAllowed"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-user-reg-switch"
              data-testid="user-reg-switch"
              value={realm.registrationAllowed ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.registrationAllowed}
              isDisabled={savingFields.has("registrationAllowed")}
              aria-busy={savingFields.has("registrationAllowed")}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  { registrationAllowed: value },
                  "registrationAllowed",
                );
              }}
              aria-label={t("registrationAllowed")}
            />
          </FormGroup>
          <FormGroup
            label={t("resetPasswordAllowed")}
            fieldId="kc-forgot-pw-switch"
            labelIcon={
              <HelpItem
                helpText={t("forgotPasswordHelpText")}
                fieldLabelId="resetPasswordAllowed"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-forgot-pw-switch"
              data-testid="forgot-pw-switch"
              name="resetPasswordAllowed"
              value={realm.resetPasswordAllowed ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.resetPasswordAllowed}
              isDisabled={savingFields.has("resetPasswordAllowed")}
              aria-busy={savingFields.has("resetPasswordAllowed")}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  { resetPasswordAllowed: value },
                  "resetPasswordAllowed",
                );
              }}
              aria-label={t("resetPasswordAllowed")}
            />
          </FormGroup>
          <FormGroup
            label={t("rememberMe")}
            fieldId="kc-remember-me-switch"
            labelIcon={
              <HelpItem
                helpText={t("rememberMeHelpText")}
                fieldLabelId="rememberMe"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-remember-me-switch"
              data-testid="remember-me-switch"
              value={realm.rememberMe ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.rememberMe}
              isDisabled={savingFields.has("rememberMe")}
              aria-busy={savingFields.has("rememberMe")}
              onChange={async (_event, value) => {
                await updateSwitchValue({ rememberMe: value }, "rememberMe");
              }}
              aria-label={t("rememberMe")}
            />
          </FormGroup>
          {passkeysVisible && (
            <FormGroup
              label={t("webAuthnPolicyPasskeysEnabled")}
              fieldId="kc-passkeys-enabled-switch"
              labelIcon={
                <HelpItem
                  helpText={t("webAuthnPolicyPasskeysEnabledHelp")}
                  fieldLabelId="webAuthnPolicyPasskeysEnabled"
                />
              }
              hasNoPaddingTop
            >
              <Switch
                id="kc-passkeys-enabled-switch"
                data-testid="passkeys-enabled-switch"
                value={
                  realm.webAuthnPolicyPasswordlessPasskeysEnabled ? "on" : "off"
                }
                label={t("on")}
                labelOff={t("off")}
                isChecked={
                  realm.webAuthnPolicyPasswordlessPasskeysEnabled ?? false
                }
                isDisabled={savingFields.has(
                  "webAuthnPolicyPasswordlessPasskeysEnabled",
                )}
                aria-busy={savingFields.has(
                  "webAuthnPolicyPasswordlessPasskeysEnabled",
                )}
                onChange={async (_event, value) => {
                  await updateSwitchValue(
                    {
                      webAuthnPolicyPasswordlessPasskeysEnabled: value,
                    },
                    "webAuthnPolicyPasswordlessPasskeysEnabled",
                  );
                }}
                aria-label={t("webAuthnPolicyPasskeysEnabled")}
              />{" "}
              <SettingsShortcut
                tooltip={t("passkeysSettingsTooltip")}
                to={{
                  ...toAuthentication({
                    realm: realmName,
                    tab: "policies",
                  }),
                  search: `?tab=${WEBAUTHN_PASSWORDLESS_POLICY}`,
                }}
              />
            </FormGroup>
          )}
        </FormAccess>
      </FormPanel>
      <FormPanel className="kc-email-settings" title={t("emailSettings")}>
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("registrationEmailAsUsername")}
            fieldId="kc-email-as-username-switch"
            labelIcon={
              <HelpItem
                helpText={t("emailAsUsernameHelpText")}
                fieldLabelId="registrationEmailAsUsername"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-email-as-username-switch"
              data-testid="email-as-username-switch"
              value={realm.registrationEmailAsUsername ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.registrationEmailAsUsername}
              isDisabled={savingFields.has("registrationEmailAsUsername")}
              aria-busy={savingFields.has("registrationEmailAsUsername")}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  [
                    {
                      registrationEmailAsUsername: value,
                    },
                    {
                      duplicateEmailsAllowed: false,
                    },
                  ],
                  "registrationEmailAsUsername",
                );
              }}
              aria-label={t("registrationEmailAsUsername")}
            />
          </FormGroup>
          <FormGroup
            label={t("loginWithEmailAllowed")}
            fieldId="kc-login-with-email-switch"
            labelIcon={
              <HelpItem
                helpText={t("loginWithEmailHelpText")}
                fieldLabelId="loginWithEmailAllowed"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-login-with-email-switch"
              data-testid="login-with-email-switch"
              value={realm.loginWithEmailAllowed ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.loginWithEmailAllowed}
              isDisabled={savingFields.has("loginWithEmailAllowed")}
              aria-busy={savingFields.has("loginWithEmailAllowed")}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  [
                    {
                      loginWithEmailAllowed: value,
                    },
                    { duplicateEmailsAllowed: false },
                  ],
                  "loginWithEmailAllowed",
                );
              }}
              aria-label={t("loginWithEmailAllowed")}
            />
          </FormGroup>
          <FormGroup
            label={t("duplicateEmailsAllowed")}
            fieldId="kc-duplicate-emails-switch"
            labelIcon={
              <HelpItem
                helpText={t("duplicateEmailsHelpText")}
                fieldLabelId="duplicateEmailsAllowed"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-duplicate-emails-switch"
              data-testid="duplicate-emails-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.duplicateEmailsAllowed}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  {
                    duplicateEmailsAllowed: value,
                  },
                  "duplicateEmailsAllowed",
                );
              }}
              isDisabled={
                realm.loginWithEmailAllowed ||
                realm.registrationEmailAsUsername ||
                savingFields.has("duplicateEmailsAllowed")
              }
              aria-busy={savingFields.has("duplicateEmailsAllowed")}
              aria-label={t("duplicateEmailsAllowed")}
            />
          </FormGroup>
          <FormGroup
            label={t("verifyEmail")}
            fieldId="kc-verify-email-switch"
            labelIcon={
              <HelpItem
                helpText={t("verifyEmailHelpText")}
                fieldLabelId="verifyEmail"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-verify-email-switch"
              data-testid="verify-email-switch"
              name="verifyEmail"
              value={realm.verifyEmail ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.verifyEmail}
              isDisabled={savingFields.has("verifyEmail")}
              aria-busy={savingFields.has("verifyEmail")}
              onChange={async (_event, value) => {
                await updateSwitchValue({ verifyEmail: value }, "verifyEmail");
              }}
              aria-label={t("verifyEmail")}
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
            label={t("editUsernameAllowed")}
            fieldId="kc-edit-username-switch"
            labelIcon={
              <HelpItem
                helpText={t("editUsernameHelp")}
                fieldLabelId="editUsernameAllowed"
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-edit-username-switch"
              data-testid="edit-username-switch"
              value={realm.editUsernameAllowed ? "on" : "off"}
              label={t("on")}
              labelOff={t("off")}
              isChecked={realm.editUsernameAllowed}
              isDisabled={savingFields.has("editUsernameAllowed")}
              aria-busy={savingFields.has("editUsernameAllowed")}
              onChange={async (_event, value) => {
                await updateSwitchValue(
                  { editUsernameAllowed: value },
                  "editUsernameAllowed",
                );
              }}
              aria-label={t("editUsernameAllowed")}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
