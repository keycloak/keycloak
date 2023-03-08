import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  ActionListItem,
  Alert,
  AlertActionLink,
  AlertVariant,
  Button,
  Checkbox,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { PasswordInput } from "../components/password-input/PasswordInput";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUser } from "../user/routes/User";
import { emailRegexPattern } from "../util";
import { useCurrentUser } from "../utils/useCurrentUser";
import useToggle from "../utils/useToggle";

import "./realm-settings-section.css";

type RealmSettingsEmailTabProps = {
  realm: RealmRepresentation;
};

export const RealmSettingsEmailTab = ({
  realm: initialRealm,
}: RealmSettingsEmailTabProps) => {
  const { t } = useTranslation("realm-settings");
  const { adminClient } = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const currentUser = useCurrentUser();

  const [realm, setRealm] = useState(initialRealm);
  const {
    register,
    control,
    handleSubmit,
    watch,
    reset: resetForm,
    getValues,
    formState: { errors },
  } = useForm<RealmRepresentation>({ defaultValues: realm });

  const reset = () => resetForm(realm);
  const watchFromValue = watch("smtpServer.from", "");
  const watchHostValue = watch("smtpServer.host", "");
  const [isTesting, toggleTest] = useToggle();

  const authenticationEnabled = useWatch({
    control,
    name: "smtpServer.auth",
    defaultValue: "",
  });

  const save = async (form: RealmRepresentation) => {
    try {
      const savedRealm = { ...realm, ...form };

      // For default value, back end is expecting null instead of empty string
      if (savedRealm.smtpServer?.port === "") savedRealm.smtpServer.port = null;

      await adminClient.realms.update({ realm: realmName }, savedRealm);
      setRealm(savedRealm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
  };

  const testConnection = async () => {
    const toNumber = (value: string) => Number(value);
    const toBoolean = (value: string) => value === true.toString();
    const valueMapper = new Map<string, (value: string) => unknown>([
      ["port", toNumber],
      ["ssl", toBoolean],
      ["starttls", toBoolean],
      ["auth", toBoolean],
    ]);

    const serverSettings = { ...getValues()["smtpServer"] };

    for (const [key, mapperFn] of valueMapper.entries()) {
      serverSettings[key] = mapperFn(serverSettings[key]);
    }

    // For default value, back end is expecting null instead of 0
    if (serverSettings.port === 0) serverSettings.port = null;

    try {
      toggleTest();
      await adminClient.realms.testSMTPConnection(
        { realm: realm.realm! },
        serverSettings
      );
      addAlert(t("testConnectionSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:testConnectionError", error);
    }
    toggleTest();
  };

  return (
    <PageSection variant="light">
      <FormPanel title={t("template")} className="kc-email-template">
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("from")}
            fieldId="kc-display-name"
            isRequired
            validated={errors.smtpServer?.from ? "error" : "default"}
            helperTextInvalid={t("users:emailInvalid")}
          >
            <KeycloakTextInput
              type="email"
              id="kc-sender-email-address"
              data-testid="sender-email-address"
              placeholder="Sender email address"
              validated={errors.smtpServer?.from ? "error" : "default"}
              {...register("smtpServer.from", {
                pattern: emailRegexPattern,
                required: true,
              })}
            />
          </FormGroup>
          <FormGroup
            label={t("fromDisplayName")}
            fieldId="kc-from-display-name"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:fromDisplayName")}
                fieldLabelId="realm-settings:authentication"
              />
            }
          >
            <KeycloakTextInput
              id="kc-from-display-name"
              data-testid="from-display-name"
              placeholder="Display name for Sender email address"
              {...register("smtpServer.fromDisplayName")}
            />
          </FormGroup>
          <FormGroup
            label={t("replyTo")}
            fieldId="kc-reply-to"
            validated={errors.smtpServer?.replyTo ? "error" : "default"}
            helperTextInvalid={t("users:emailInvalid")}
          >
            <KeycloakTextInput
              type="email"
              id="kc-reply-to"
              placeholder="Reply to email address"
              validated={errors.smtpServer?.replyTo ? "error" : "default"}
              {...register("smtpServer.replyTo", {
                pattern: emailRegexPattern,
              })}
            />
          </FormGroup>
          <FormGroup
            label={t("replyToDisplayName")}
            fieldId="kc-reply-to-display-name"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:replyToDisplayName")}
                fieldLabelId="realm-settings:replyToDisplayName"
              />
            }
          >
            <KeycloakTextInput
              id="kc-reply-to-display-name"
              placeholder='Display name for "reply to" email address'
              {...register("smtpServer.replyToDisplayName")}
            />
          </FormGroup>
          <FormGroup
            label={t("envelopeFrom")}
            fieldId="kc-envelope-from"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:envelopeFrom")}
                fieldLabelId="realm-settings:envelopeFrom"
              />
            }
          >
            <KeycloakTextInput
              id="kc-envelope-from"
              placeholder="Sender envelope email address"
              {...register("smtpServer.envelopeFrom")}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        className="kc-email-connection"
        title={t("connectionAndAuthentication")}
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("host")}
            fieldId="kc-host"
            isRequired
            validated={errors.smtpServer?.host ? "error" : "default"}
            helperTextInvalid={t("common:required")}
          >
            <KeycloakTextInput
              id="kc-host"
              placeholder="SMTP host"
              validated={errors.smtpServer?.host ? "error" : "default"}
              {...register("smtpServer.host", { required: true })}
            />
          </FormGroup>
          <FormGroup label={t("port")} fieldId="kc-port">
            <KeycloakTextInput
              id="kc-port"
              placeholder="SMTP port (defaults to 25)"
              {...register("smtpServer.port")}
            />
          </FormGroup>
          <FormGroup label={t("encryption")} fieldId="kc-html-display-name">
            <Controller
              name="smtpServer.ssl"
              control={control}
              defaultValue="false"
              render={({ field }) => (
                <Checkbox
                  id="kc-enable-ssl"
                  data-testid="enable-ssl"
                  label={t("enableSSL")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange("" + value)}
                />
              )}
            />
            <Controller
              name="smtpServer.starttls"
              control={control}
              defaultValue="false"
              render={({ field }) => (
                <Checkbox
                  id="kc-enable-start-tls"
                  data-testid="enable-start-tls"
                  label={t("enableStartTLS")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange("" + value)}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("authentication")}
            fieldId="kc-authentication"
          >
            <Controller
              name="smtpServer.auth"
              control={control}
              defaultValue=""
              render={({ field }) => (
                <Switch
                  id="kc-authentication-switch"
                  data-testid="email-authentication-switch"
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={field.value === "true"}
                  onChange={(value) => {
                    field.onChange("" + value);
                  }}
                  aria-label={t("authentication")}
                />
              )}
            />
          </FormGroup>
          {authenticationEnabled === "true" && (
            <>
              <FormGroup
                label={t("username")}
                fieldId="kc-username"
                isRequired
                validated={errors.smtpServer?.user ? "error" : "default"}
                helperTextInvalid={t("common:required")}
              >
                <KeycloakTextInput
                  id="kc-username"
                  data-testid="username-input"
                  placeholder="Login username"
                  validated={errors.smtpServer?.user ? "error" : "default"}
                  {...register("smtpServer.user", { required: true })}
                />
              </FormGroup>
              <FormGroup
                label={t("password")}
                fieldId="kc-username"
                isRequired
                validated={errors.smtpServer?.password ? "error" : "default"}
                helperTextInvalid={t("common:required")}
                labelIcon={
                  <HelpItem
                    helpText={t("realm-settings-help:password")}
                    fieldLabelId="realm-settings:password"
                  />
                }
              >
                <PasswordInput
                  id="kc-password"
                  data-testid="password-input"
                  aria-label={t("password")}
                  validated={errors.smtpServer?.password ? "error" : "default"}
                  {...register("smtpServer.password", { required: true })}
                />
              </FormGroup>
            </>
          )}
          {currentUser && (
            <FormGroup id="descriptionTestConnection">
              {currentUser.email ? (
                <Alert
                  variant="info"
                  isInline
                  title={t("testConnectionHint.withEmail", {
                    email: currentUser.email,
                  })}
                />
              ) : (
                <Alert
                  variant="warning"
                  isInline
                  title={t("testConnectionHint.withoutEmail", {
                    userName: currentUser.username,
                  })}
                  actionLinks={
                    <AlertActionLink
                      component={(props) => (
                        <Link
                          {...props}
                          to={toUser({
                            realm: realmName,
                            id: currentUser.id!,
                            tab: "settings",
                          })}
                        />
                      )}
                    >
                      {t("testConnectionHint.withoutEmailAction")}
                    </AlertActionLink>
                  }
                />
              )}
            </FormGroup>
          )}
          <ActionGroup>
            <ActionListItem>
              <Button
                variant="primary"
                type="submit"
                data-testid="email-tab-save"
              >
                {t("common:save")}
              </Button>
            </ActionListItem>
            <ActionListItem>
              <Button
                variant="secondary"
                onClick={() => testConnection()}
                data-testid="test-connection-button"
                isDisabled={
                  !(emailRegexPattern.test(watchFromValue) && watchHostValue) ||
                  !currentUser?.email
                }
                aria-describedby="descriptionTestConnection"
                isLoading={isTesting}
                spinnerAriaValueText={t("testingConnection")}
              >
                {t("common:testConnection")}
              </Button>
            </ActionListItem>
            <ActionListItem>
              <Button
                variant="link"
                onClick={reset}
                data-testid="email-tab-revert"
              >
                {t("common:revert")}
              </Button>
            </ActionListItem>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
