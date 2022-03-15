import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import React, { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  TextInput,
  Form,
  Switch,
  TextArea,
  Select,
  SelectVariant,
  SelectOption,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { ClientDescription } from "./ClientDescription";
import { CapabilityConfig } from "./add/CapabilityConfig";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { SaveReset } from "./advanced/SaveReset";
import { SamlConfig } from "./add/SamlConfig";
import { SamlSignature } from "./add/SamlSignature";
import environment from "../environment";
import { useRealm } from "../context/realm-context/RealmContext";

type ClientSettingsProps = {
  client: ClientRepresentation;
  save: () => void;
  reset: () => void;
};

export const ClientSettings = ({
  client,
  save,
  reset,
}: ClientSettingsProps) => {
  const { register, control, watch, errors } =
    useFormContext<ClientRepresentation>();
  const { t } = useTranslation("clients");
  const { realm } = useRealm();

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const loginThemes = useServerInfo().themes!["login"];
  const consentRequired = watch("consentRequired");
  const displayOnConsentScreen: string = watch(
    "attributes.display.on.consent.screen"
  );
  const protocol = watch("protocol");
  const frontchannelLogout = watch("frontchannelLogout");
  const idpInitiatedSsoUrlName: string = watch(
    "attributes.saml_idp_initiated_sso_url_name"
  );

  const sections = useMemo(() => {
    let result = ["generalSettings", "accessSettings"];

    if (protocol === "saml") {
      result = [...result, "samlCapabilityConfig", "signatureAndEncryption"];
    } else if (!client.bearerOnly) {
      result = [...result, "capabilityConfig"];
    } else {
      return result;
    }

    return [...result, "loginSettings", "logoutSettings"];
  }, [protocol, client]);

  return (
    <ScrollForm
      className="pf-u-px-lg"
      sections={sections.map((section) => t(section))}
    >
      <Form isHorizontal>
        <ClientDescription protocol={client.protocol} />
      </Form>
      {protocol === "saml" ? (
        <SamlConfig />
      ) : (
        !client.bearerOnly && <CapabilityConfig />
      )}
      {protocol === "saml" && <SamlSignature />}
      <FormAccess isHorizontal role="manage-clients">
        {!client.bearerOnly && (
          <>
            <FormGroup
              label={t("rootUrl")}
              fieldId="kc-root-url"
              labelIcon={
                <HelpItem
                  helpText="clients-help:rootUrl"
                  fieldLabelId="clients:rootUrl"
                />
              }
            >
              <TextInput
                type="text"
                id="kc-root-url"
                name="rootUrl"
                ref={register}
              />
            </FormGroup>
            <FormGroup
              label={t("homeURL")}
              fieldId="kc-home-url"
              labelIcon={
                <HelpItem
                  helpText="clients-help:homeURL"
                  fieldLabelId="clients:homeURL"
                />
              }
            >
              <TextInput
                type="text"
                id="kc-home-url"
                name="baseUrl"
                ref={register}
              />
            </FormGroup>
            <FormGroup
              label={t("validRedirectUri")}
              fieldId="kc-redirect"
              labelIcon={
                <HelpItem
                  helpText="clients-help:validRedirectURIs"
                  fieldLabelId="clients:validRedirectUri"
                />
              }
            >
              <MultiLineInput
                name="redirectUris"
                aria-label={t("validRedirectUri")}
                addButtonLabel="clients:addRedirectUri"
              />
            </FormGroup>
            {protocol === "saml" && (
              <>
                <FormGroup
                  label={t("idpInitiatedSsoUrlName")}
                  fieldId="idpInitiatedSsoUrlName"
                  labelIcon={
                    <HelpItem
                      helpText="clients-help:idpInitiatedSsoUrlName"
                      fieldLabelId="clients:idpInitiatedSsoUrlName"
                    />
                  }
                  helperText={
                    idpInitiatedSsoUrlName !== "" &&
                    t("idpInitiatedSsoUrlNameHelp", {
                      url: `${environment.authServerUrl}/realms/${realm}/protocol/saml/clients/${idpInitiatedSsoUrlName}`,
                    })
                  }
                >
                  <TextInput
                    type="text"
                    id="idpInitiatedSsoUrlName"
                    name="attributes.saml_idp_initiated_sso_url_name"
                    ref={register}
                  />
                </FormGroup>
                <FormGroup
                  label={t("idpInitiatedSsoRelayState")}
                  fieldId="idpInitiatedSsoRelayState"
                  labelIcon={
                    <HelpItem
                      helpText="clients-help:idpInitiatedSsoRelayState"
                      fieldLabelId="clients:idpInitiatedSsoRelayState"
                    />
                  }
                >
                  <TextInput
                    type="text"
                    id="idpInitiatedSsoRelayState"
                    name="attributes.saml_idp_initiated_sso_relay_state"
                    ref={register}
                  />
                </FormGroup>
                <FormGroup
                  label={t("masterSamlProcessingUrl")}
                  fieldId="masterSamlProcessingUrl"
                  labelIcon={
                    <HelpItem
                      helpText="clients-help:masterSamlProcessingUrl"
                      fieldLabelId="clients:masterSamlProcessingUrl"
                    />
                  }
                >
                  <TextInput
                    type="text"
                    id="masterSamlProcessingUrl"
                    name="adminUrl"
                    ref={register}
                  />
                </FormGroup>
              </>
            )}
            {protocol !== "saml" && (
              <FormGroup
                label={t("webOrigins")}
                fieldId="kc-web-origins"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:webOrigins"
                    fieldLabelId="clients:webOrigins"
                  />
                }
              >
                <MultiLineInput
                  name="webOrigins"
                  aria-label={t("webOrigins")}
                  addButtonLabel="clients:addWebOrigins"
                />
              </FormGroup>
            )}
          </>
        )}
        {protocol !== "saml" && (
          <FormGroup
            label={t("adminURL")}
            fieldId="kc-admin-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:adminURL"
                fieldLabelId="clients:adminURL"
              />
            }
          >
            <TextInput
              type="text"
              id="kc-admin-url"
              name="adminUrl"
              ref={register}
            />
          </FormGroup>
        )}
        {client.bearerOnly && (
          <SaveReset
            className="keycloak__form_actions"
            name="settings"
            save={save}
            reset={reset}
          />
        )}
      </FormAccess>
      <FormAccess isHorizontal role="manage-clients">
        <FormGroup
          label={t("loginTheme")}
          labelIcon={
            <HelpItem
              helpText="clients-help:loginTheme"
              fieldLabelId="clients:loginTheme"
            />
          }
          fieldId="loginTheme"
        >
          <Controller
            name="attributes.login_theme"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="loginTheme"
                onToggle={setLoginThemeOpen}
                onSelect={(_, value) => {
                  onChange(value.toString());
                  setLoginThemeOpen(false);
                }}
                selections={value || t("common:choose")}
                variant={SelectVariant.single}
                aria-label={t("loginTheme")}
                isOpen={loginThemeOpen}
              >
                {[
                  <SelectOption key="empty" value="">
                    {t("common:choose")}
                  </SelectOption>,
                  ...loginThemes.map((theme) => (
                    <SelectOption
                      selected={theme.name === value}
                      key={theme.name}
                      value={theme.name}
                    />
                  )),
                ]}
              </Select>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("consentRequired")}
          labelIcon={
            <HelpItem
              helpText="clients-help:consentRequired"
              fieldLabelId="clients:consentRequired"
            />
          }
          fieldId="kc-consent"
          hasNoPaddingTop
        >
          <Controller
            name="consentRequired"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id="kc-consent-switch"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value}
                onChange={onChange}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("displayOnClient")}
          labelIcon={
            <HelpItem
              helpText="clients-help:displayOnClient"
              fieldLabelId="clients:displayOnClient"
            />
          }
          fieldId="kc-display-on-client"
          hasNoPaddingTop
        >
          <Controller
            name="attributes.display.on.consent.screen"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id="kc-display-on-client-switch"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value === "true"}
                onChange={(value) => onChange("" + value)}
                isDisabled={!consentRequired}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("consentScreenText")}
          labelIcon={
            <HelpItem
              helpText="clients-help:consentScreenText"
              fieldLabelId="clients:consentScreenText"
            />
          }
          fieldId="kc-consent-screen-text"
        >
          <TextArea
            id="kc-consent-screen-text"
            name="attributes.consent.screen.text"
            ref={register}
            isDisabled={!(consentRequired && displayOnConsentScreen === "true")}
          />
        </FormGroup>
      </FormAccess>
      <FormAccess isHorizontal role="manage-clients">
        {protocol === "openid-connect" && (
          <>
            <FormGroup
              label={t("frontchannelLogout")}
              labelIcon={
                <HelpItem
                  helpText="clients-help:frontchannelLogout"
                  fieldLabelId="clients:frontchannelLogout"
                />
              }
              fieldId="frontchannelLogout"
              hasNoPaddingTop
            >
              <Controller
                name="frontchannelLogout"
                defaultValue={true}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    id="frontchannelLogout"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value.toString() === "true"}
                    onChange={(value) => onChange(value.toString())}
                  />
                )}
              />
            </FormGroup>
            {frontchannelLogout?.toString() === "true" && (
              <FormGroup
                label={t("frontchannelLogoutUrl")}
                fieldId="frontchannelLogoutUrl"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:frontchannelLogoutUrl"
                    fieldLabelId="clients:frontchannelLogoutUrl"
                  />
                }
                helperTextInvalid={
                  errors.attributes?.frontchannel?.logout?.url?.message
                }
                validated={
                  errors.attributes?.frontchannel?.logout?.url?.message
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              >
                <TextInput
                  type="text"
                  id="frontchannelLogoutUrl"
                  name="attributes.frontchannel.logout.url"
                  ref={register({
                    validate: (uri) =>
                      ((uri.startsWith("https://") ||
                        uri.startsWith("http://")) &&
                        !uri.includes("*")) ||
                      uri === "" ||
                      t("frontchannelUrlInvalid").toString(),
                  })}
                  validated={
                    errors.attributes?.frontchannel?.logout?.url?.message
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
            )}
          </>
        )}
        <FormGroup
          label={t("backchannelLogoutUrl")}
          fieldId="backchannelLogoutUrl"
          labelIcon={
            <HelpItem
              helpText="clients-help:backchannelLogoutUrl"
              fieldLabelId="clients:backchannelLogoutUrl"
            />
          }
          helperTextInvalid={
            errors.attributes?.backchannel?.logout?.url?.message
          }
          validated={
            errors.attributes?.backchannel?.logout?.url?.message
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <TextInput
            type="text"
            id="backchannelLogoutUrl"
            name="attributes.backchannel.logout.url"
            ref={register({
              validate: (uri) =>
                ((uri.startsWith("https://") || uri.startsWith("http://")) &&
                  !uri.includes("*")) ||
                uri === "" ||
                t("backchannelUrlInvalid").toString(),
            })}
            validated={
              errors.attributes?.backchannel?.logout?.url?.message
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          label={t("backchannelLogoutSessionRequired")}
          labelIcon={
            <HelpItem
              helpText="clients-help:backchannelLogoutSessionRequired"
              fieldLabelId="clients:backchannelLogoutSessionRequired"
            />
          }
          fieldId="backchannelLogoutSessionRequired"
          hasNoPaddingTop
        >
          <Controller
            name="attributes.backchannel.logout.session.required"
            defaultValue="true"
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id="backchannelLogoutSessionRequired"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value === "true"}
                onChange={(value) => onChange(value.toString())}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("backchannelLogoutRevokeOfflineSessions")}
          labelIcon={
            <HelpItem
              helpText="clients-help:backchannelLogoutRevokeOfflineSessions"
              fieldLabelId="clients:backchannelLogoutRevokeOfflineSessions"
            />
          }
          fieldId="backchannelLogoutRevokeOfflineSessions"
          hasNoPaddingTop
        >
          <Controller
            name="attributes.backchannel.logout.revoke.offline.tokens"
            defaultValue="false"
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id="backchannelLogoutRevokeOfflineSessions"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value === "true"}
                onChange={(value) => onChange(value.toString())}
              />
            )}
          />
        </FormGroup>
        <SaveReset
          className="keycloak__form_actions"
          name="settings"
          save={save}
          reset={reset}
        />
      </FormAccess>
    </ScrollForm>
  );
};
