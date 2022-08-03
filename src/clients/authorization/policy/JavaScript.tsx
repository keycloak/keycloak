import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";

import { HelpItem } from "../../../components/help-enabler/HelpItem";

export const JavaScript = () => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("code")}
      labelIcon={
        <HelpItem
          helpText="clients-help:policyCode"
          fieldLabelId="clients:code"
        />
      }
      fieldId="code"
      isRequired
    >
      <Controller
        name="code"
        defaultValue=""
        control={control}
        render={({ onChange, value }) => (
          <CodeEditor
            id="code"
            data-testid="code"
            type="text"
            onChange={onChange}
            code={value}
            height="600px"
            language={Language.javascript}
          />
        )}
      />
    </FormGroup>
  );
};
