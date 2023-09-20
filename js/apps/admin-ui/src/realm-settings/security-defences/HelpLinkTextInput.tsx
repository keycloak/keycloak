import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { Trans, useTranslation } from "react-i18next";

import { FormattedLink } from "../../components/external-link/FormattedLink";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

type HelpLinkTextInputProps = {
  fieldName: string;
  url: string;
};

export const HelpLinkTextInput = ({
  fieldName,
  url,
}: HelpLinkTextInputProps) => {
  const { t } = useTranslation();
  const { register } = useFormContext();
  const name = fieldName.substr(fieldName.indexOf(".") + 1);
  return (
    <FormGroup
      label={t(name)}
      fieldId={name}
      labelIcon={
        <HelpItem
          helpText={
            <Trans i18nKey={`${name}Help`}>
              Default value prevents pages from being included
              <FormattedLink href={url} title={t("learnMore")} />
            </Trans>
          }
          fieldLabelId={name}
        />
      }
    >
      <KeycloakTextInput id={name} {...register(fieldName)} />
    </FormGroup>
  );
};
