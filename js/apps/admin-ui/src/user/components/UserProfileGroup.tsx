import { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { FormGroup } from "@patternfly/react-core";
import { PropsWithChildren } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { label } from "../utils";

export type UserProfileFieldsProps = UserProfileAttribute & {
  roles?: string[];
};

type LengthValidator =
  | {
      min: number;
    }
  | undefined;

const isRequired = (attribute: UserProfileAttribute) =>
  Object.keys(attribute.required || {}).length !== 0 ||
  (((attribute.validators?.length as LengthValidator)?.min as number) || 0) > 0;

export const UserProfileGroup = ({
  children,
  ...attribute
}: PropsWithChildren<UserProfileFieldsProps>) => {
  const { t } = useTranslation("users");
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
      validated={errors.username ? "error" : "default"}
      helperTextInvalid={t("common:required")}
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
