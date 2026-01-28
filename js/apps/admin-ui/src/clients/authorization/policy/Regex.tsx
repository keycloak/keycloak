import { useTranslation } from "react-i18next";
import { SwitchControl, TextControl } from "@keycloak/keycloak-ui-shared";

export const Regex = () => {
  const { t } = useTranslation();

  return (
    <>
      <TextControl
        name="targetClaim"
        label={t("targetClaim")}
        labelIcon={t("targetClaimHelp")}
        rules={{ required: t("required") }}
      />
      <TextControl
        name="pattern"
        label={t("regexPattern")}
        labelIcon={t("regexPatternHelp")}
        rules={{ required: t("required") }}
      />
      <SwitchControl
        name="targetContextAttributes"
        label={t("targetContextAttributes")}
        labelIcon={t("targetContextAttributesHelp")}
        labelOn={t("yes")}
        labelOff={t("no")}
      />
    </>
  );
};
