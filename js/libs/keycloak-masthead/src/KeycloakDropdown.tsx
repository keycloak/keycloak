import {
  Dropdown,
  DropdownProps,
  DropdownToggle,
  KebabToggle,
  KebabToggleProps,
} from "@patternfly/react-core/deprecated";
import { ReactNode, useState } from "react";

type KeycloakDropdownProps = Omit<DropdownProps, "toggle"> & {
  isKebab?: boolean;
  title?: ReactNode;
  dropDownItems: ReactNode[];
};

export const KeycloakDropdown = ({
  isKebab = false,
  title,
  dropDownItems,
  ...rest
}: KeycloakDropdownProps) => {
  const [open, setOpen] = useState(false);
  const onToggle: KebabToggleProps["onToggle"] = (_, isOpen) => setOpen(isOpen);

  return (
    <Dropdown
      {...rest}
      isPlain
      position="right"
      toggle={
        isKebab ? (
          <KebabToggle onToggle={onToggle}>{title}</KebabToggle>
        ) : (
          <DropdownToggle onToggle={onToggle}>{title}</DropdownToggle>
        )
      }
      isOpen={open}
      dropdownItems={dropDownItems}
    />
  );
};
