import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, InputGroup } from "@patternfly/react-core";
import { EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";

import {
  KeycloakTextInput,
  KeycloakTextInputProps,
} from "../keycloak-text-input/KeycloakTextInput";

type PasswordInputProps = KeycloakTextInputProps & {
  hasReveal?: boolean;
};

const PasswordInputBase = ({
  hasReveal = true,
  innerRef,
  ...rest
}: PasswordInputProps) => {
  const { t } = useTranslation("common-help");
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

export const PasswordInput = React.forwardRef(
  (props: PasswordInputProps, ref: React.Ref<HTMLInputElement>) => (
    <PasswordInputBase
      {...props}
      innerRef={ref as React.MutableRefObject<any>}
    />
  )
);
PasswordInput.displayName = "PasswordInput";
