import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";

import { HelpItem } from "@keycloak/keycloak-ui-shared";

export const JavaScript = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("code")}
      labelIcon={
        <HelpItem helpText={t("policyCodeHelp")} fieldLabelId="code" />
      }
      fieldId="code"
      isRequired
    >
      <Controller
        name="code"
        defaultValue=""
        control={control}
        render={({ field }) => (
          <CodeEditor
            id="code"
            data-testid="code"
            onChange={field.onChange}
            code={field.value}
            height="600px"
            language={Language.javascript}
            isReadOnly={true}
          />
        )}
      />
    </FormGroup>
  );
};
