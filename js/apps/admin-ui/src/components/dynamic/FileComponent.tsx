import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FileUpload, FormGroup } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";

export const FileComponent = ({
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
  const [filename, setFilename] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Controller
        name={convertToName(name!)}
        control={control}
        defaultValue={defaultValue || ""}
        render={({ field }) => (
          <FileUpload
            id={name!}
            value={field.value}
            type="text"
            filename={filename}
            isDisabled={isDisabled}
            onFileInputChange={(_, file) => setFilename(file.name)}
            onReadStarted={() => setIsLoading(true)}
            onReadFinished={() => setIsLoading(false)}
            onClearClick={() => {
              field.onChange("");
              setFilename("");
            }}
            isLoading={isLoading}
            allowEditingUploadedText={false}
            onTextChange={(value) => {
              field.onChange(value);
            }}
            onDataChange={(_, value) => {
              field.onChange(value);
            }}
          />
        )}
      />
    </FormGroup>
  );
};
