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
        label={t("user") + " " + t("id") + " " + t("claim")}
        labelIcon={t("userIDClaimHelp")}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.userNameClaim"
        label={t("username") + " " + t("claim")}
        labelIcon={t("userNameClaimHelp")}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.emailClaim"
        label={t("email") + " " + t("claim")}
        labelIcon={t("emailClaimHelp")}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.fullNameClaim"
        label={t("name") + " " + t("claim")}
        labelIcon={t("fullNameClaimHelp")}
      />
      <TextControl
        name="config.givenNameClaim"
        label={t("firstName") + " " + t("claim")}
        labelIcon={t("givenNameClaimHelp")}
      />
      <TextControl
        name="config.familyNameClaim"
        label={t("lastName") + " " + t("claim")}
        labelIcon={t("familyNameClaimHelp")}
      />
    </Form>
  );
};
