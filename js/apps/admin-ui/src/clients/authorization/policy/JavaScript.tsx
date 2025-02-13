import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import CodeEditor from "@uiw/react-textarea-code-editor";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

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
          <div style={{ height: "600px", overflow: "scroll" }}>
            <CodeEditor
              id="code"
              data-testid="code"
              readOnly
              value={field.value}
              language="js"
            />
          </div>
        )}
      />
    </FormGroup>
  );
};
