import { FormGroup, InputGroup } from "@patternfly/react-core";
import { get } from "lodash-es";
import { PropsWithChildren } from "react";
import { UseFormReturn } from "react-hook-form";
import { HelpItem } from "../controls/HelpItem";
import {
  UserFormFields,
  UserProfileAttributeMetadata,
} from "./userProfileConfig";
import {
  TranslationFunction,
  fieldName,
  isRequiredAttribute,
  label,
} from "./utils";

export type UserProfileGroupProps = {
  t: TranslationFunction;
  form: UseFormReturn<UserFormFields>;
  attribute: UserProfileAttributeMetadata;
  renderer?: (
    attribute: UserProfileAttributeMetadata,
  ) => JSX.Element | undefined;
};

export const UserProfileGroup = ({
  t,
  form,
  attribute,
  renderer,
  children,
}: PropsWithChildren<UserProfileGroupProps>) => {
  const helpText = attribute.annotations?.["inputHelperTextBefore"] as string;
  const {
    formState: { errors },
  } = form;

  return (
    <FormGroup
      key={attribute.name}
      label={label(attribute, t) || ""}
      fieldId={attribute.name}
      isRequired={isRequiredAttribute(attribute)}
      validated={get(errors, fieldName(attribute.name)) ? "error" : "default"}
      helperTextInvalid={t(get(errors, fieldName(attribute.name))?.message)}
      labelIcon={
        helpText ? (
          <HelpItem helpText={helpText} fieldLabelId={attribute.name!} />
        ) : undefined
      }
    >
      <InputGroup>
        {children}
        {renderer?.(attribute)}
      </InputGroup>
    </FormGroup>
  );
};
