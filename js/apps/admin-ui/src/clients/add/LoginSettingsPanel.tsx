import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const LoginSettingsPanel = ({ access }: { access?: boolean }) => {
  const { t } = useTranslation();
  const { register, control, watch } = useFormContext<FormFields>();

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const loginThemes = useServerInfo().themes!["login"];
  const consentRequired = watch("consentRequired");
  const displayOnConsentScreen: string = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.display.on.consent.screen",
    ),
  );

  return (
    <FormAccess isHorizontal fineGrainedAccess={access} role="manage-clients">
      <FormGroup
        label={t("loginTheme")}
        labelIcon={
          <HelpItem helpText={t("loginThemeHelp")} fieldLabelId="loginTheme" />
        }
        fieldId="loginTheme"
      >
        <Controller
          name="attributes.login_theme"
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId="loginTheme"
              onToggle={setLoginThemeOpen}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setLoginThemeOpen(false);
              }}
              selections={field.value || t("choose")}
              variant={SelectVariant.single}
              aria-label={t("loginTheme")}
              isOpen={loginThemeOpen}
            >
              {[
                <SelectOption key="empty" value="">
                  {t("choose")}
                </SelectOption>,
                ...loginThemes.map((theme) => (
                  <SelectOption
                    selected={theme.name === field.value}
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
            helpText={t("consentRequiredHelp")}
            fieldLabelId="consentRequired"
          />
        }
        fieldId="kc-consent"
        hasNoPaddingTop
      >
        <Controller
          name="consentRequired"
          defaultValue={false}
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-consent-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value}
              onChange={field.onChange}
              aria-label={t("consentRequired")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("displayOnClient")}
        labelIcon={
          <HelpItem
            helpText={t("displayOnClientHelp")}
            fieldLabelId="displayOnClient"
          />
        }
        fieldId="kc-display-on-client"
        hasNoPaddingTop
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.display.on.consent.screen",
          )}
          defaultValue={false}
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-display-on-client-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange("" + value)}
              isDisabled={!consentRequired}
              aria-label={t("displayOnClient")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("consentScreenText")}
        labelIcon={
          <HelpItem
            helpText={t("consentScreenTextHelp")}
            fieldLabelId="consentScreenText"
          />
        }
        fieldId="kc-consent-screen-text"
      >
        <KeycloakTextArea
          id="kc-consent-screen-text"
          {...register(
            convertAttributeNameToForm<FormFields>(
              "attributes.consent.screen.text",
            ),
          )}
          isDisabled={!(consentRequired && displayOnConsentScreen === "true")}
        />
      </FormGroup>
    </FormAccess>
  );
};
