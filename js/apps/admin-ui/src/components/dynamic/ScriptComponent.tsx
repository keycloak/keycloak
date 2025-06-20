import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import CodeEditor from "@uiw/react-textarea-code-editor";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const ScriptComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  required,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem
          helpText={<span style={{ whiteSpace: "pre-wrap" }}>{helpText}</span>}
          fieldLabelId={`${label}`}
        />
      }
      fieldId={name!}
      isRequired={required}
    >
      <Controller
        name={convertToName(name!)}
        defaultValue={defaultValue}
        control={control}
        render={({ field }) => (
          <div style={{ height: "600px", overflow: "scroll" }}>
            <CodeEditor
              id={name!}
              data-testid={name}
              readOnly={isDisabled}
              onChange={field.onChange}
              value={Array.isArray(field.value) ? field.value[0] : field.value}
              language="js"
            />
          </div>
        )}
      />
    </FormGroup>
  );
};
