import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import CodeEditor from "../form/CodeEditor";
import type { ComponentProps } from "./components";

export const ScriptComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  required,
  isDisabled = false,
  convertToName,
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
          <CodeEditor
            id={name!}
            data-testid={name}
            readOnly={isDisabled}
            onChange={field.onChange}
            value={Array.isArray(field.value) ? field.value[0] : field.value}
            language="js"
            height={600}
          />
        )}
      />
    </FormGroup>
  );
};
