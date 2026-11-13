import {
  FileUpload,
  ValidatedOptions,
  FileUploadProps,
} from "@patternfly/react-core";
import { ReactNode, useState } from "react";
import {
  FieldPath,
  FieldValues,
  PathValue,
  UseControllerProps,
  useController,
} from "react-hook-form";
import { getRuleValue } from "../utils/getRuleValue";
import { FormLabel } from "./FormLabel";
import { useTranslation } from "react-i18next";

export type FileUploadControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = UseControllerProps<T, P> &
  Omit<FileUploadProps, "name" | "isRequired" | "required"> & {
    label: string;
    labelIcon?: string | ReactNode;
    isDisabled?: boolean;
    "data-testid"?: string;
    type?: string;
  };

export const FileUploadControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: FileUploadControlProps<T, P>,
) => {
  const { labelIcon, ...rest } = props;
  const required = !!getRuleValue(props.rules?.required);
  const defaultValue = props.defaultValue ?? ("" as PathValue<T, P>);

  const { t } = useTranslation();

  const [filename, setFilename] = useState<string>("");

  const { field, fieldState } = useController({
    ...props,
    defaultValue,
  });

  // PatternFly exposes no props for the visible filename input. A callback ref
  // keeps the ARIA state scoped to this instance and updates it on each render.
  const associateError = (container: HTMLDivElement | null) => {
    const input = container?.querySelector<HTMLInputElement>("input[readonly]");
    input?.setAttribute("aria-invalid", String(!!fieldState.error));
    if (fieldState.error) {
      input?.setAttribute("aria-errormessage", `${props.id}-filename-error`);
    } else {
      input?.removeAttribute("aria-errormessage");
    }
  };

  return (
    <FormLabel
      id={`${props.id}-filename`}
      name={props.name}
      label={props.label}
      labelIcon={labelIcon}
      isRequired={required}
      error={fieldState.error}
    >
      <div ref={associateError}>
        <FileUpload
          isRequired={required}
          data-testid={props["data-testid"] || props.name}
          filename={filename}
          browseButtonText={t("browse")}
          validated={
            fieldState.error ? ValidatedOptions.error : ValidatedOptions.default
          }
          hideDefaultPreview
          isDisabled={props.isDisabled}
          type="text"
          onFileInputChange={(_, file) => {
            field.onChange(file);
            setFilename(file.name);
          }}
          onClearClick={() => {
            field.onChange(null);
            setFilename("");
          }}
          {...rest}
          {...field}
          filenameAriaLabel={props.filenameAriaLabel ?? props.label}
        />
      </div>
    </FormLabel>
  );
};
