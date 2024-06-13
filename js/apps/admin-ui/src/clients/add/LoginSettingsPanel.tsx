import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl, TextAreaControl } from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormAccess } from "../../components/form/FormAccess";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const LoginSettingsPanel = ({ access }: { access?: boolean }) => {
  const { t } = useTranslation();
  const { watch } = useFormContext<FormFields>();

  const loginThemes = useServerInfo().themes!["login"];
  const consentRequired = watch("consentRequired");
  const displayOnConsentScreen: string = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.display.on.consent.screen",
    ),
  );

  return (
    <FormAccess isHorizontal fineGrainedAccess={access} role="manage-clients">
      <SelectControl
        name="attributes.login_theme"
        label={t("loginTheme")}
        labelIcon={t("loginThemeHelp")}
        controller={{
          defaultValue: "",
        }}
        options={[
          { key: "", value: t("choose") },
          ...loginThemes.map(({ name }) => ({ key: name, value: name })),
        ]}
      />
      <DefaultSwitchControl
        name="consentRequired"
        label={t("consentRequired")}
        labelIcon={t("consentRequiredHelp")}
      />
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.display.on.consent.screen",
        )}
        label={t("displayOnClient")}
        labelIcon={t("displayOnClientHelp")}
        isDisabled={!consentRequired}
        stringify
      />
      <TextAreaControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.consent.screen.text",
        )}
        label={t("consentScreenText")}
        labelIcon={t("consentScreenTextHelp")}
        isDisabled={!(consentRequired && displayOnConsentScreen === "true")}
      />
    </FormAccess>
  );
};
