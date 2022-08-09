import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FileUpload, FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const FileComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();
  const [filename, setFilename] = useState("");

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <Controller
        name={convertToName(name!)}
        control={control}
        defaultValue={defaultValue || ""}
        render={({ onChange, value }) => (
          <FileUpload
            id={name!}
            value={value}
            filename={filename}
            isDisabled={isDisabled}
            onChange={(value, filename) => {
              onChange(value);
              setFilename(filename);
            }}
          />
        )}
      />
    </FormGroup>
  );
};
