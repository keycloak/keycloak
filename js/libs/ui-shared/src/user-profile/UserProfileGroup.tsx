import { FormGroup } from "@patternfly/react-core";
import { PropsWithChildren } from "react";
import { useFormContext } from "react-hook-form";
import { HelpItem } from "../controls/HelpItem";
import { UserProfileAttribute } from "./userProfileConfig";
import { TranslationFunction, label } from "./utils";

export type UserProfileFieldsProps = UserProfileAttribute & {
  t: TranslationFunction;
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
  const { t } = attribute;
  const helpText = attribute.annotations?.["inputHelperTextBefore"] as string;

  const {
    formState: { errors },
  } = useFormContext();

  console.log("label", label(attribute));
  return (
    <FormGroup
      key={attribute.name}
      label={label(attribute) || ""}
      fieldId={attribute.name}
      isRequired={isRequired(attribute)}
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
