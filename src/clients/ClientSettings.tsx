import React, { useState } from "react";
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

type ClientSettingsProps = {
  save: () => void;
  reset: () => void;
};

export const ClientSettings = ({ save, reset }: ClientSettingsProps) => {
  const { register, control, watch } = useFormContext();
  const { t } = useTranslation("clients");

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const loginThemes = useServerInfo().themes!["login"];
  const consentRequired: boolean = watch("consentRequired");
  const displayOnConsentScreen: string = watch(
    "attributes.display-on-consent-screen"
  );

  return (
    <>
      <ScrollForm
        className="pf-u-px-lg"
        sections={[
          t("generalSettings"),
          t("capabilityConfig"),
          t("accessSettings"),
          t("loginSettings"),
        ]}
      >
        <Form isHorizontal>
          <ClientDescription />
        </Form>
        <CapabilityConfig />
        <FormAccess isHorizontal role="manage-clients">
          <FormGroup
            label={t("rootUrl")}
            fieldId="kc-root-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:rootUrl"
                forLabel={t("rootUrl")}
                forID="kc-root-url"
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
            label={t("validRedirectUri")}
            fieldId="kc-redirect"
            labelIcon={
              <HelpItem
                helpText="clients-help:validRedirectURIs"
                forLabel={t("validRedirectUri")}
                forID={t(`common:helpLabel`, { label: t("validRedirectUri") })}
              />
            }
          >
            <MultiLineInput
              name="redirectUris"
              aria-label={t("validRedirectUri")}
              addButtonLabel="clients:addRedirectUri"
            />
          </FormGroup>
          <FormGroup
            label={t("homeURL")}
            fieldId="kc-home-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:homeURL"
                forLabel={t("homeURL")}
                forID={t(`common:helpLabel`, { label: t("homeURL") })}
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
            label={t("webOrigins")}
            fieldId="kc-web-origins"
            labelIcon={
              <HelpItem
                helpText="clients-help:webOrigins"
                forLabel={t("webOrigins")}
                forID={t(`common:helpLabel`, { label: t("webOrigins") })}
              />
            }
          >
            <MultiLineInput
              name="webOrigins"
              aria-label={t("webOrigins")}
              addButtonLabel="clients:addWebOrigins"
            />
          </FormGroup>
          <FormGroup
            label={t("adminURL")}
            fieldId="kc-admin-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:adminURL"
                forLabel={t("adminURL")}
                forID="kc-admin-url"
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
        </FormAccess>
        <FormAccess isHorizontal role="manage-clients">
          <FormGroup
            label={t("loginTheme")}
            labelIcon={
              <HelpItem
                helpText="clients-help:loginTheme"
                forLabel={t("loginTheme")}
                forID="loginTheme"
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
                  onToggle={() => setLoginThemeOpen(!loginThemeOpen)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setLoginThemeOpen(false);
                  }}
                  selections={value || t("common:choose")}
                  variant={SelectVariant.single}
                  aria-label={t("loginTheme")}
                  isOpen={loginThemeOpen}
                >
                  <SelectOption key="empty" value="">
                    {t("common:choose")}
                  </SelectOption>
                  <>
                    {loginThemes?.map((theme) => (
                      <SelectOption
                        selected={theme.name === value}
                        key={theme.name}
                        value={theme.name}
                      />
                    ))}
                  </>
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("consentRequired")}
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
            fieldId="kc-display-on-client"
            hasNoPaddingTop
          >
            <Controller
              name="attributes.display-on-consent-screen"
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
            fieldId="kc-consent-screen-text"
          >
            <TextArea
              id="kc-consent-screen-text"
              name="attributes.consent-screen-text"
              ref={register}
              isDisabled={
                !(consentRequired && displayOnConsentScreen === "true")
              }
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
    </>
  );
};
