import {
  TextInputProps,
  InputGroup,
  InputGroupItem,
} from "@patternfly/react-core";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type ColorControlProps = TextInputProps & {
  name: string;
  label: string;
  color: string;
  onUserChange?: () => void;
};

export const ColorControl = ({
  name,
  color,
  label,
  onUserChange,
  ...props
}: ColorControlProps) => {
  const { t } = useTranslation();
  const { control, setValue } = useFormContext();
  const currentValue = useWatch({
    control,
    name,
  });

  const handleChange = (value: string) => {
    setValue(name, value);
    if (onUserChange) {
      onUserChange();
    }
  };

  return (
    <InputGroup>
      <InputGroupItem isFill>
        <TextControl {...props} name={name} label={t(label)} />
      </InputGroupItem>
      <input
        type="color"
        value={currentValue || color}
        onChange={(e) => handleChange(e.target.value)}
      />
    </InputGroup>
  );
};
