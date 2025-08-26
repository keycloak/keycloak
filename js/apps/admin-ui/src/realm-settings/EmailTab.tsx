import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  ActionListItem,
  Alert,
  AlertActionLink,
  AlertVariant,
  Button,
  Checkbox,
  Radio,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  FormPanel,
  PasswordControl,
  SwitchControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../components/form/FormAccess";
import { toUser } from "../user/routes/User";
import { emailRegexPattern } from "../util";
import { useCurrentUser } from "../utils/useCurrentUser";
import useToggle from "../utils/useToggle";

import "./realm-settings-section.css";

type RealmSettingsEmailTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

type FormFields = Omit<RealmRepresentation, "users" | "federatedUsers">;

export const RealmSettingsEmailTab = ({
  realm,
  save,
}: RealmSettingsEmailTabProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const currentUser = useCurrentUser();

  const form = useForm<FormFields>({ defaultValues: realm });
  const { control, handleSubmit, watch, reset: resetForm, getValues } = form;

  const reset = () => resetForm(realm);
  const watchFromValue = watch("smtpServer.from", "");
  const watchHostValue = watch("smtpServer.host", "");
  const [isTesting, toggleTest] = useToggle();

  const authenticationEnabled = useWatch({
    control,
    name: "smtpServer.auth",
    defaultValue: realm.smtpServer?.auth || "false",
  });

  const authType = useWatch({
    control,
    name: "smtpServer.authType",
    defaultValue: realm.smtpServer?.authType || "basic",
  });

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
        serverSettings,
      );
      addAlert(t("testConnectionSuccess"), AlertVariant.success);
    } catch (error) {
      addError("testConnectionError", error);
    }
    toggleTest();
  };

  return (
    <PageSection variant="light">
      <FormProvider {...form}>
        <FormPanel title={t("template")} className="kc-email-template">
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-v5-u-mt-lg"
            onSubmit={handleSubmit(save)}
          >
            <TextControl
              name="smtpServer.from"
              label={t("from")}
              type="email"
              placeholder={t("smtpFromPlaceholder")}
              rules={{
                pattern: {
                  value: emailRegexPattern,
                  message: t("emailInvalid"),
                },
                required: t("required"),
              }}
            />
            <TextControl
              name="smtpServer.fromDisplayName"
              label={t("fromDisplayName")}
              labelIcon={t("fromDisplayNameHelp")}
              placeholder={t("smtpFromDisplayPlaceholder")}
            />
            <TextControl
              name="smtpServer.replyTo"
              label={t("replyTo")}
              type="email"
              placeholder={t("replyToEmailPlaceholder")}
              rules={{
                pattern: {
                  value: emailRegexPattern,
                  message: t("emailInvalid"),
                },
              }}
            />
            <TextControl
              name="smtpServer.replyToDisplayName"
              label={t("replyToDisplayName")}
              labelIcon={t("replyToDisplayNameHelp")}
              placeholder={t("replyToDisplayPlaceholder")}
            />
            <TextControl
              name="smtpServer.envelopeFrom"
              label={t("envelopeFrom")}
              labelIcon={t("envelopeFromHelp")}
              placeholder={t("senderEnvelopePlaceholder")}
            />
          </FormAccess>
        </FormPanel>
        <FormPanel
          className="kc-email-connection"
          title={t("connectionAndAuthentication")}
        >
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-v5-u-mt-lg"
            onSubmit={handleSubmit(save)}
          >
            <TextControl
              name="smtpServer.host"
              label={t("host")}
              rules={{
                required: t("required"),
              }}
            />
            <TextControl
              name="smtpServer.port"
              label={t("port")}
              placeholder={t("smtpPortPlaceholder")}
            />
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
                    onChange={(_event, value) => field.onChange("" + value)}
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
                    onChange={(_event, value) => field.onChange("" + value)}
                  />
                )}
              />
            </FormGroup>
            <SwitchControl
              name="smtpServer.auth"
              label={t("authentication")}
              data-testid="smtpServer.auth"
              defaultValue=""
              labelOn={t("enabled")}
              labelOff={t("disabled")}
              stringify
            />
            {authenticationEnabled === "true" && (
              <>
                <TextControl
                  name="smtpServer.user"
                  label={t("username")}
                  placeholder={t("loginUsernamePlaceholder")}
                  rules={{
                    required: t("required"),
                  }}
                />
                <FormGroup label={t("authenticationType")} fieldId="authType">
                  <Controller
                    name="smtpServer.authType"
                    control={control}
                    defaultValue="basic"
                    render={({ field }) => (
                      <>
                        <Radio
                          id="basicAuth"
                          name="smtpServer.authType"
                          data-testid="smtpServer.authType.basic"
                          label={t("authenticationTypeBasicAuth")}
                          value="basic"
                          isChecked={field.value === "basic"}
                          onChange={() => field.onChange("basic")}
                        />
                        <Radio
                          id="tokenAuth"
                          name="smtpServer.authType"
                          data-testid="smtpServer.authType.token"
                          label={t("authenticationTypeTokenAuth")}
                          value="token"
                          isChecked={field.value === "token"}
                          onChange={() => field.onChange("token")}
                        />
                      </>
                    )}
                  />
                </FormGroup>
                {authType === "basic" && (
                  <PasswordControl
                    name="smtpServer.password"
                    label={t("password")}
                    labelIcon={t("passwordHelp")}
                    rules={{
                      required: t("required"),
                    }}
                  />
                )}
                {authType === "token" && (
                  <>
                    <TextControl
                      name="smtpServer.authTokenUrl"
                      label={t("authTokenUrl")}
                      helperText={t("tokenTokenUrlHelp")}
                      rules={{
                        required: t("required"),
                      }}
                    />
                    <TextControl
                      name="smtpServer.authTokenScope"
                      label={t("authTokenScope")}
                      helperText={t("authTokenScopeHelp")}
                      rules={{
                        required: t("required"),
                      }}
                    />
                    <TextControl
                      name="smtpServer.authTokenClientId"
                      label={t("authTokenClientId")}
                      helperText={t("authTokenClientIdHelp")}
                      rules={{
                        required: t("required"),
                      }}
                    />
                    <PasswordControl
                      name="smtpServer.authTokenClientSecret"
                      label={t("authTokenClientSecret")}
                      labelIcon={t("authTokenClientSecretHelp")}
                      rules={{
                        required: t("required"),
                      }}
                    />
                  </>
                )}
              </>
            )}
            <SwitchControl
              name="smtpServer.allowutf8"
              label={t("allowutf8")}
              labelIcon={t("allowutf8Help")}
              data-testid="smtpServer.allowutf8"
              defaultValue=""
              labelOn={t("enabled")}
              labelOff={t("disabled")}
              stringify
            />
            <Controller
              name="smtpServer.debug"
              control={control}
              defaultValue="false"
              render={({ field }) => (
                <Checkbox
                  id="kc-enable-debug"
                  data-testid="enable-debug"
                  label={t("enableDebugSMTP")}
                  isChecked={field.value === "true"}
                  onChange={(_event, value) => field.onChange("" + value)}
                />
              )}
            />
            {currentUser && (
              <FormGroup id="descriptionTestConnection">
                {currentUser.email ? (
                  <Alert
                    variant="info"
                    component="h2"
                    isInline
                    title={t("testConnectionHint.withEmail", {
                      email: currentUser.email,
                    })}
                  />
                ) : (
                  <Alert
                    variant="warning"
                    component="h2"
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
                              realm: currentUser.realm!,
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
                  {t("save")}
                </Button>
              </ActionListItem>
              <ActionListItem>
                <Button
                  variant="secondary"
                  onClick={() => testConnection()}
                  data-testid="test-connection-button"
                  isDisabled={
                    !(
                      emailRegexPattern.test(watchFromValue) && watchHostValue
                    ) || !currentUser?.email
                  }
                  aria-describedby="descriptionTestConnection"
                  isLoading={isTesting}
                  spinnerAriaValueText={t("testingConnection")}
                >
                  {t("testConnection")}
                </Button>
              </ActionListItem>
              <ActionListItem>
                <Button
                  variant="link"
                  onClick={reset}
                  data-testid="email-tab-revert"
                >
                  {t("revert")}
                </Button>
              </ActionListItem>
            </ActionGroup>
          </FormAccess>
        </FormPanel>
      </FormProvider>
    </PageSection>
  );
};
