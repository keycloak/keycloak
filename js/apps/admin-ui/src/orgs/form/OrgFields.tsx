import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { HelpItem } from "../../components/help-enabler/HelpItem";

export const OrgFields = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <>
      <FormGroup label="Name" fieldId="name">
        <Controller
          name="name"
          control={control}
          render={({ field }) => (
            <TextInput
              id="name"
              value={field.value}
              onChange={field.onChange}
              data-testid="name-input"
            />
          )}
        />
      </FormGroup>

      <FormGroup label="Display name" fieldId="displayName">
        <Controller
          name="displayName"
          control={control}
          render={({ field }) => (
            <TextInput
              id="displayName"
              value={field.value}
              onChange={field.onChange}
              data-testid="displayName-input"
            />
          )}
        />
      </FormGroup>

      {/*Domains*/}
      <FormGroup
        label={t("domains")}
        fieldId="domains"
        labelIcon={<HelpItem helpText="domainHelp" fieldLabelId="domain" />}
      >
        <MultiLineInput
          name="domains"
          aria-label={t("domains")}
          addButtonLabel="addDomain"
        />
      </FormGroup>

      <FormGroup label="URL" fieldId="url">
        <Controller
          name="url"
          control={control}
          render={({ field }) => (
            <TextInput
              id="url"
              value={field.value}
              onChange={field.onChange}
              data-testid="url-input"
            />
          )}
        />
      </FormGroup>
    </>
  );
};
