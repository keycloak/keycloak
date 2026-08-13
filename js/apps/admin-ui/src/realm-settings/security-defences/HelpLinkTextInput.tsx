import { Trans, useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { FormattedLink } from "../../components/external-link/FormattedLink";

type HelpLinkTextInputProps = {
  fieldName: string;
  url: string;
};

export const HelpLinkTextInput = ({
  fieldName,
  url,
}: HelpLinkTextInputProps) => {
  const { t } = useTranslation();
  const name = fieldName.substring(fieldName.indexOf(".") + 1);
  return (
    <TextControl
      name={fieldName}
      label={t(name)}
      labelIcon={
        <Trans
          i18nKey={`${name}Help`}
          components={{
            formattedlink: <FormattedLink href={url} title={t("learnMore")} />,
          }}
        />
      }
    />
  );
};
