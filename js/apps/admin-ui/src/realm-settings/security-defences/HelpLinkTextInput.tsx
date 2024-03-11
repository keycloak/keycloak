import { FormGroup } from "@patternfly/react-core";
import { FormProvider, useFormContext } from "react-hook-form";
import { Trans, useTranslation } from "react-i18next";
import { FormattedLink } from "../../components/external-link/FormattedLink";
import { HelpItem, TextControl } from "ui-shared";

type HelpLinkTextInputProps = {
  fieldName: string;
  url: string;
};

export const HelpLinkTextInput = ({
  fieldName,
  url,
}: HelpLinkTextInputProps) => {
  const { t } = useTranslation();
  const form = useFormContext();
  const name = fieldName.slice(fieldName.indexOf(".") + 1);
  return (
    <FormProvider {...form}>
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
        <TextControl name={fieldName} />
      </FormGroup>
    </FormProvider>
  );
};
