import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { Form, Title } from "@patternfly/react-core";

export const UserProfileClaimsSettings = () => {
  const { t } = useTranslation();

  return (
    <Form isHorizontal className="pf-v5-u-py-lg">
      <Title headingLevel="h2" size="xl" className="kc-form-panel__title">
        {t("userProfileClaims")}
      </Title>
      <TextControl
        name="config.userIDClaim"
        label={t("userIDClaim")}
        labelIcon={t("userIDClaimHelp")}
        rules={{
          required: t("required"),
        }}
        defaultValue={"sub"}
      />
      <TextControl
        name="config.userNameClaim"
        label={t("userNameClaim")}
        labelIcon={t("userNameClaimHelp")}
        rules={{
          required: t("required"),
        }}
        defaultValue={"preferred_username"}
      />
      <TextControl
        name="config.emailClaim"
        label={t("emailClaim")}
        labelIcon={t("emailClaimHelp")}
        rules={{
          required: t("required"),
        }}
        defaultValue={"email"}
      />
      <TextControl
        name="config.fullNameClaim"
        label={t("fullNameClaim")}
        labelIcon={t("fullNameClaimHelp")}
        defaultValue={"name"}
      />
      <TextControl
        name="config.givenNameClaim"
        label={t("givenNameClaim")}
        labelIcon={t("givenNameClaimHelp")}
        defaultValue={"given_name"}
      />
      <TextControl
        name="config.familyNameClaim"
        label={t("familyNameClaim")}
        labelIcon={t("familyNameClaimHelp")}
        defaultValue={"family_name"}
      />
    </Form>
  );
};
