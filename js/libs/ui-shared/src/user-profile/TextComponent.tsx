import {
  Button,
  Flex,
  FlexItem,
  TextInput,
  TextInputTypes,
  Tooltip,
} from "@patternfly/react-core";
import { TrashIcon } from "@patternfly/react-icons";

import { UserProfileFieldProps } from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import { fieldName, isRequiredAttribute, label } from "./utils";

export const TextComponent = (props: UserProfileFieldProps) => {
  const { form, inputType, attribute, t } = props;
  const isRequired = isRequiredAttribute(attribute);
  const type = inputType.startsWith("html")
    ? (inputType.substring("html".length + 2) as TextInputTypes)
    : "text";

  const isEmailPending = attribute.name === "kc.email.pending";
  const hasEmailPendingValue =
    isEmailPending &&
    form.getValues(fieldName(attribute.name)) &&
    form.getValues(fieldName(attribute.name)) !== "";

  const handleRemovePendingEmail = () => {
    // Set the field value to empty string to effectively remove it
    form.setValue(fieldName(attribute.name), "");
    const event = new CustomEvent("removePendingEmailVerification", {
      detail: { attributeName: attribute.name },
    });
    window.dispatchEvent(event);
  };

  return (
    <UserProfileGroup {...props}>
      {hasEmailPendingValue ? (
        <Flex
          spaceItems={{ default: "spaceItemsSm" }}
          alignItems={{ default: "alignItemsCenter" }}
        >
          <FlexItem flex={{ default: "flex_1" }}>
            <TextInput
              id={attribute.name}
              data-testid={attribute.name}
              type={type}
              value={
                form.getValues(fieldName(attribute.name)) ||
                attribute.defaultValue ||
                ""
              }
              readOnly
            />
          </FlexItem>
          <FlexItem>
            <Tooltip content={t("removePendingEmailVerification")}>
              <Button
                variant="link"
                icon={<TrashIcon />}
                onClick={handleRemovePendingEmail}
                data-testid="remove-pending-email"
                aria-label={t("removePendingEmailVerification")}
              />
            </Tooltip>
          </FlexItem>
        </Flex>
      ) : isEmailPending ? null : (
        <TextInput
          id={attribute.name}
          data-testid={attribute.name}
          type={type}
          placeholder={
            attribute.readOnly
              ? ""
              : label(
                  t,
                  attribute.annotations?.["inputTypePlaceholder"] as string,
                  "",
                  attribute.annotations?.[
                    "inputOptionLabelsI18nPrefix"
                  ] as string,
                )
          }
          isDisabled={attribute.readOnly}
          isRequired={isRequired}
          defaultValue={attribute.defaultValue}
          {...form.register(fieldName(attribute.name))}
        />
      )}
    </UserProfileGroup>
  );
};
