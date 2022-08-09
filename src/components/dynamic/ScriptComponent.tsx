import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";

import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const ScriptComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem
          helpText={<span style={{ whiteSpace: "pre-wrap" }}>{helpText}</span>}
          fieldLabelId={`dynamic:${label}`}
        />
      }
      fieldId={name!}
    >
      <Controller
        name={convertToName(name!)}
        defaultValue={defaultValue}
        control={control}
        render={({ onChange, value }) => (
          <CodeEditor
            id={name!}
            data-testid={name}
            isReadOnly={isDisabled}
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
