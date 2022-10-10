import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";
import { FileUpload } from "../json-file-upload/patternfly/FileUpload";

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
  const [isLoading, setIsLoading] = useState(false);

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
            type="text"
            filename={filename}
            isDisabled={isDisabled}
            onFileInputChange={(_, file) => setFilename(file.name)}
            onReadStarted={() => setIsLoading(true)}
            onReadFinished={() => setIsLoading(false)}
            onClearClick={() => {
              onChange("");
              setFilename("");
            }}
            isLoading={isLoading}
            allowEditingUploadedText={false}
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
