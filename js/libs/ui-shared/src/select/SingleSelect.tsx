import {
  MenuToggle,
  Select,
  SelectList,
  SelectOptionProps,
} from "@patternfly/react-core";
import { Children, useRef, useState } from "react";
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
  className,
  isDisabled,
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

  const childArray = Children.toArray(
    children,
  ) as React.ReactElement<SelectOptionProps>[];

  return (
    <Select
      ref={ref}
      maxMenuHeight={propertyToString(maxHeight)}
      isScrollable
      popperProps={{
        appendTo: append(),
        direction,
        width: propertyToString(width),
      }}
      {...props}
      onClick={toggle}
      onOpenChange={(isOpen) => {
        if (isOpen !== open) toggle();
      }}
      selected={selections}
      onSelect={(_, value) => {
        onSelect?.(value || "");
        toggle();
      }}
      toggle={(ref) => (
        <MenuToggle
          id={toggleId}
          ref={ref}
          className={className}
          onClick={toggle}
          isExpanded={isOpen}
          aria-label={props["aria-label"]}
          icon={toggleIcon}
          isDisabled={isDisabled}
          isFullWidth
        >
          {childArray.find((c) => c.props.value === selections)?.props
            .children ||
            selections ||
            props["aria-label"]}
        </MenuToggle>
      )}
      isOpen={isOpen}
    >
      <SelectList>{children}</SelectList>
    </Select>
  );
};
