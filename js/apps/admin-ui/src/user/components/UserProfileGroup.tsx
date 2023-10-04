import { UserProfileAttributeMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { FormGroup } from "@patternfly/react-core";
import { PropsWithChildren } from "react";
import { UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { UserFormFields } from "../form-state";
import { label } from "../utils";
import { isRequiredAttribute } from "../utils/user-profile";

export type UserProfileGroupProps = {
  form: UseFormReturn<UserFormFields>;
  attribute: UserProfileAttributeMetadata;
};

export const UserProfileGroup = ({
  form,
  attribute,
  children,
}: PropsWithChildren<UserProfileGroupProps>) => {
  const { t } = useTranslation();
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
      validated={errors.username ? "error" : "default"}
      helperTextInvalid={t("required")}
      labelIcon={
        helpText ? (
          <HelpItem helpText={helpText} fieldLabelId={attribute.name!} />
        ) : undefined
      }
    >
      {children}
    </FormGroup>
  );
};
