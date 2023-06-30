import { ReactNode, useState } from "react";
import {
  Dropdown,
  DropdownProps,
  DropdownToggle,
  KebabToggle,
} from "@patternfly/react-core";

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

  return (
    <Dropdown
      {...rest}
      isPlain
      position="right"
      toggle={
        isKebab ? (
          <KebabToggle onToggle={setOpen}>{title}</KebabToggle>
        ) : (
          <DropdownToggle onToggle={setOpen}>{title}</DropdownToggle>
        )
      }
      isOpen={open}
      dropdownItems={dropDownItems}
    />
  );
};
