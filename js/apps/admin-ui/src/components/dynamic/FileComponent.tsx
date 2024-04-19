import { FormGroup } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { FileUpload } from "../json-file-upload/patternfly/FileUpload";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const FileComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
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
            onChange={(value, filename) => {
              field.onChange(value);
              setFilename(filename);
            }}
          />
        )}
      />
    </FormGroup>
  );
};
