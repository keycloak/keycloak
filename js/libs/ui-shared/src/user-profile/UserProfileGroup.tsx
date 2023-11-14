import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { FormGroup, InputGroup } from "@patternfly/react-core";
import { get } from "lodash-es";
import { PropsWithChildren, ReactNode } from "react";
import { UseFormReturn } from "react-hook-form";
import { HelpItem } from "../controls/HelpItem";
import {
  TranslationFunction,
  UserFormFields,
  fieldName,
  isRequiredAttribute,
  labelAttribute,
} from "./utils";

export type UserProfileGroupProps = {
  t: TranslationFunction;
  form: UseFormReturn<UserFormFields>;
  attribute: UserProfileAttributeMetadata;
  renderer?: (attribute: UserProfileAttributeMetadata) => ReactNode;
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

  const component = renderer?.(attribute);
  return (
    <FormGroup
      key={attribute.name}
      label={labelAttribute(t, attribute) || ""}
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
      {component ? (
        <InputGroup>
          {children}
          {component}
        </InputGroup>
      ) : (
        children
      )}
    </FormGroup>
  );
};
