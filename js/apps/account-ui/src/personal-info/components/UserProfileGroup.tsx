import { Button, FormGroup, InputGroup, Popover } from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon, HelpIcon } from "@patternfly/react-icons";
import { get } from "lodash-es";
import { PropsWithChildren } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { UserProfileAttributeMetadata } from "../../api/representations";
import { environment } from "../../environment";
import { keycloak } from "../../keycloak";
import { fieldName, label } from "../utils";
import { TFuncKey } from "../../i18n";

export type UserProfileFieldsProps = UserProfileAttributeMetadata;

type LengthValidator =
  | {
      min: number;
    }
  | undefined;

const isRequired = (attribute: UserProfileAttributeMetadata) =>
  Object.keys(attribute.required || {}).length !== 0 ||
  (((attribute.validators.length as LengthValidator)?.min as number) || 0) > 0;

export const UserProfileGroup = ({
  children,
  ...attribute
}: PropsWithChildren<UserProfileFieldsProps>) => {
  const { t } = useTranslation("translation");
  const helpText = attribute.annotations?.["inputHelperTextBefore"] as string;

  const {
    formState: { errors },
  } = useFormContext();

  const {
    updateEmailFeatureEnabled,
    updateEmailActionEnabled,
    isRegistrationEmailAsUsername,
    isEditUserNameAllowed,
  } = environment.features;

  return (
    <FormGroup
      key={attribute.name}
      label={label(attribute, t) || ""}
      fieldId={attribute.name}
      isRequired={isRequired(attribute)}
      validated={get(errors, fieldName(attribute)) ? "error" : "default"}
      helperTextInvalid={t(
        get(errors, fieldName(attribute))?.message as TFuncKey,
      )}
      labelIcon={
        helpText ? (
          <Popover bodyContent={helpText}>
            <HelpIcon data-testid={`${attribute.name}-help`} />
          </Popover>
        ) : undefined
      }
    >
      <InputGroup>
        {children}
        {attribute.name === "email" &&
          updateEmailFeatureEnabled &&
          updateEmailActionEnabled &&
          (!isRegistrationEmailAsUsername || isEditUserNameAllowed) && (
            <Button
              id="update-email-btn"
              variant="link"
              onClick={() => keycloak.login({ action: "UPDATE_EMAIL" })}
              icon={<ExternalLinkSquareAltIcon />}
              iconPosition="right"
            >
              {t("updateEmail")}
            </Button>
          )}
      </InputGroup>
    </FormGroup>
  );
};
