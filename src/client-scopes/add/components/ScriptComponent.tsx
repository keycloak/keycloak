import React from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";

export const ScriptComponent = ({
  name,
  label,
  helpText,
  defaultValue,
}: ComponentProps) => {
  const { t } = useTranslation("client-scopes");
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} forLabel={t(label!)} forID={name!} />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.${name?.replaceAll(".", "-")}`}
        defaultValue={defaultValue}
        control={control}
        render={({ onChange, value }) => (
          <CodeEditor
            id={name!}
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
