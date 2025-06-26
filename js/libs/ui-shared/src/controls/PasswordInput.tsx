import {
  Button,
  InputGroup,
  InputGroupItem,
  TextInput,
  type TextInputProps,
} from "@patternfly/react-core";
import { EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";
import { MutableRefObject, Ref, forwardRef, useState } from "react";
import { useTranslation } from "react-i18next";

export type PasswordInputProps = TextInputProps & {
  hasReveal?: boolean;
  isTideIdp?: boolean;
};

const PasswordInputBase = ({
  hasReveal = true,
  innerRef,
  isTideIdp = false,
  ...rest
}: PasswordInputProps) => {
  const { t } = useTranslation();
  const [hidePassword, setHidePassword] = useState(true);
  return (
    <>
      {/** TIDECLOAK IMPLEMENTATION */}
      <InputGroup style={{ display: isTideIdp ? 'none' : undefined }}>
        <InputGroupItem isFill>
          <TextInput
            {...rest}
            type={hidePassword ? "password" : "text"}
            ref={innerRef}
          />
        </InputGroupItem>
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
    </>
  );
};

export const PasswordInput = forwardRef(
  (props: PasswordInputProps, ref: Ref<HTMLInputElement>) => (
    <PasswordInputBase {...props} innerRef={ref as MutableRefObject<any>} />
  ),
);
PasswordInput.displayName = "PasswordInput";
