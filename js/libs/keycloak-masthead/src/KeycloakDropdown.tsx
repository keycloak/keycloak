import { ReactNode, useState } from "react";
import { Dropdown, DropdownToggle, KebabToggle } from "@patternfly/react-core";

type KeycloakDropdownProps = {
  isKebab?: boolean;
  title?: ReactNode;
  dropDownItems: ReactNode[];
};

export const KeycloakDropdown = ({
  isKebab = false,
  title,
  dropDownItems,
}: KeycloakDropdownProps) => {
  const [open, setOpen] = useState(false);

  return (
    <Dropdown
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
