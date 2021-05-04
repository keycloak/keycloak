import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  InputGroup,
  TextInput,
  TextInputProps,
} from "@patternfly/react-core";
import { EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";

const PasswordInputBase = ({ innerRef, ...rest }: TextInputProps) => {
  const { t } = useTranslation("common-help");
  const [hidePassword, setHidePassword] = useState(true);
  return (
    <InputGroup>
      <TextInput
        {...rest}
        type={hidePassword ? "password" : "text"}
        ref={innerRef}
      />
      <Button
        variant="control"
        aria-label={t("showPassword")}
        onClick={() => setHidePassword(!hidePassword)}
      >
        {hidePassword ? <EyeIcon /> : <EyeSlashIcon />}
      </Button>
    </InputGroup>
  );
};

export const PasswordInput = React.forwardRef(
  (props: TextInputProps, ref: React.Ref<HTMLInputElement>) => (
    <PasswordInputBase
      {...props}
      innerRef={ref as React.MutableRefObject<any>}
    />
  )
);
PasswordInput.displayName = "PasswordInput";
