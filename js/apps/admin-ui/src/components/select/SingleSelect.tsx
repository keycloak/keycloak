import { MenuToggle, Select, SelectList } from "@patternfly/react-core";
import { useRef, useState } from "react";
import { KeycloakSelectProps, propertyToString } from "./KeycloakSelect";

type SingleSelectProps = Omit<KeycloakSelectProps, "variant">;

export const SingleSelect = ({
  toggleId,
  onToggle,
  onSelect,
  selections,
  isOpen,
  menuAppendTo,
  direction,
  width,
  maxHeight,
  toggleIcon,
  children,
  ...props
}: SingleSelectProps) => {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLElement>();
  const toggle = () => {
    setOpen(!open);
    onToggle(!open);
  };

  const append = () => {
    if (menuAppendTo === "parent") {
      return ref.current?.parentElement || "inline";
    }
    return "inline";
  };

  return (
    <Select
      ref={ref}
      maxMenuHeight={propertyToString(maxHeight)}
      popperProps={{
        appendTo: append(),
        direction,
        width: propertyToString(width),
      }}
      {...props}
      onClick={toggle}
      selected={selections}
      onSelect={(_, value) => {
        onSelect?.(value || "");
        toggle();
      }}
      toggle={(ref) => (
        <MenuToggle
          id={toggleId}
          ref={ref}
          onClick={toggle}
          isExpanded={isOpen}
          aria-label={props["aria-label"]}
          icon={toggleIcon}
        >
          {selections}
        </MenuToggle>
      )}
      isOpen={isOpen}
    >
      <SelectList>{children}</SelectList>
    </Select>
  );
};
