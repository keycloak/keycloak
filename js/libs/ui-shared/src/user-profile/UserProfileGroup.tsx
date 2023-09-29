import { FormGroup, InputGroup } from "@patternfly/react-core";
import { get } from "lodash-es";
import { PropsWithChildren } from "react";
import { useFormContext } from "react-hook-form";
import { HelpItem } from "../controls/HelpItem";
import { FormFieldProps } from "./UserProfileFields";
import { UserProfileAttribute } from "./userProfileConfig";
import { fieldName, label } from "./utils";

export type UserProfileFieldsProps = Omit<FormFieldProps, "attribute"> &
  UserProfileAttribute;

type LengthValidator =
  | {
      min: number;
    }
  | undefined;

const isRequired = (attribute: UserProfileAttribute) =>
  !!attribute.required ||
  (((attribute.validators?.length as LengthValidator)?.min as number) || 0) > 0;

export const UserProfileGroup = ({
  children,
  renderer,
  ...attribute
}: PropsWithChildren<UserProfileFieldsProps>) => {
  const { t } = attribute;
  const helpText = attribute.annotations?.["inputHelperTextBefore"] as string;

  const {
    formState: { errors },
  } = useFormContext();

  return (
    <FormGroup
      key={attribute.name}
      label={label(attribute) || ""}
      fieldId={attribute.name}
      isRequired={isRequired(attribute)}
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
