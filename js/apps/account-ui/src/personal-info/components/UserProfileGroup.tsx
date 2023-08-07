import { FormGroup, Popover } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { get } from "lodash-es";
import { PropsWithChildren } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { UserProfileAttributeMetadata } from "../../api/representations";
import { fieldName, label } from "../utils";

export type UserProfileFieldsProps = UserProfileAttributeMetadata;

const isRequired = (attribute: UserProfileAttributeMetadata) =>
  Object.keys(attribute.required || {}).length !== 0 ||
  ((attribute.validators.length.min as number) || 0) > 0;

export const UserProfileGroup = ({
  children,
  ...attribute
}: PropsWithChildren<UserProfileFieldsProps>) => {
  const { t } = useTranslation("translation");
  const helpText = attribute.annotations?.["inputHelperTextBefore"] as string;

  const {
    formState: { errors },
  } = useFormContext();

  return (
    <FormGroup
      key={attribute.name}
      label={label(attribute, t) || ""}
      fieldId={attribute.name}
      isRequired={isRequired(attribute)}
      validated={get(errors, fieldName(attribute)) ? "error" : "default"}
      helperTextInvalid={get(errors, fieldName(attribute))?.message as string}
      labelIcon={
        helpText ? (
          <Popover bodyContent={helpText}>
            <HelpIcon />
          </Popover>
        ) : undefined
      }
    >
      {children}
    </FormGroup>
  );
};
