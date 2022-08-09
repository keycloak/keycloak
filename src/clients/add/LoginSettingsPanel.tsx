import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { convertAttributeNameToForm } from "../../util";

export const LoginSettingsPanel = ({ access }: { access?: boolean }) => {
  const { t } = useTranslation("clients");
  const { register, control, watch } = useFormContext<ClientRepresentation>();

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const loginThemes = useServerInfo().themes!["login"];
  const consentRequired = watch("consentRequired");
  const displayOnConsentScreen: string = watch(
    "attributes.display.on.consent.screen"
  );

  return (
    <FormAccess isHorizontal fineGrainedAccess={access} role="manage-clients">
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
          name={convertAttributeNameToForm(
            "attributes.display.on.consent.screen"
          )}
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
        <KeycloakTextArea
          id="kc-consent-screen-text"
          name={convertAttributeNameToForm("attributes.consent.screen.text")}
          ref={register}
          isDisabled={!(consentRequired && displayOnConsentScreen === "true")}
        />
      </FormGroup>
    </FormAccess>
  );
};
