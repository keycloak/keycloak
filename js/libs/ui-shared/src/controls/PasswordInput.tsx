import { Button, InputGroup } from "@patternfly/react-core";
import { EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";
import { forwardRef, MutableRefObject, Ref, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  KeycloakTextInput,
  KeycloakTextInputProps,
} from "../keycloak-text-input/KeycloakTextInput";

export type PasswordInputProps = KeycloakTextInputProps & {
  hasReveal?: boolean;
};

const PasswordInputBase = ({
  hasReveal = true,
  innerRef,
  ...rest
}: PasswordInputProps) => {
  const { t } = useTranslation();
  const [hidePassword, setHidePassword] = useState(true);
  return (
    <InputGroup>
      <KeycloakTextInput
        {...rest}
        type={hidePassword ? "password" : "text"}
        ref={innerRef}
      />
      {hasReveal && (
        <Button
          variant="control"
          aria-label={t("showPassword")}
          onClick={() => setHidePassword(!hidePassword)}
        >
          {hidePassword ? <EyeIcon /> : <EyeSlashIcon />}
        </Button>
      )}
    </InputGroup>
  );
};

export const PasswordInput = forwardRef(
  (props: PasswordInputProps, ref: Ref<HTMLInputElement>) => (
    <PasswordInputBase {...props} innerRef={ref as MutableRefObject<any>} />
  ),
);
PasswordInput.displayName = "PasswordInput";
