import {
  Label,
  LabelGroup,
  Button,
  MenuFooter,
  MenuToggle,
  MenuToggleStatus,
  Select,
  SelectList,
  SelectOptionProps,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
} from "@patternfly/react-core";

import { TimesIcon } from "@patternfly/react-icons";
import { Children, useRef, useState } from "react";
import {
  KeycloakSelectProps,
  SelectVariant,
  propertyToString,
} from "./KeycloakSelect";

export const TypeaheadSelect = ({
  toggleId,
  onSelect,
  onToggle,
  onFilter,
  variant,
  validated,
  placeholderText,
  maxHeight,
  width,
  toggleIcon,
  direction,
  selections,
  typeAheadAriaLabel,
  chipGroupComponent,
  chipGroupProps,
  footer,
  isDisabled,
  children,
  ...rest
}: KeycloakSelectProps) => {
  const [filterValue, setFilterValue] = useState("");
  const [focusedItemIndex, setFocusedItemIndex] = useState<number>(0);
  const textInputRef = useRef<HTMLInputElement>();

  const childArray = Children.toArray(
    children,
  ) as React.ReactElement<SelectOptionProps>[];

  const toggle = () => {
    onToggle?.(!rest.isOpen);
  };

  const { numChips, ...chipGroupPropsRest } = chipGroupProps || {};

  const onInputKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    const focusedItem = childArray[focusedItemIndex];
    onToggle?.(true);

    switch (event.key) {
      case "Enter": {
        event.preventDefault();

        if (variant !== SelectVariant.typeaheadMulti) {
          setFilterValue(focusedItem.props.value);
        } else {
          setFilterValue("");
        }
        onSelect?.(focusedItem.props.value);
        onToggle?.(false);
        setFocusedItemIndex(0);

        break;
      }
      case "Escape": {
        onToggle?.(false);
        break;
      }
      case "Backspace": {
        if (variant === SelectVariant.typeahead) {
          onSelect?.("");
        }
        break;
      }
      case "ArrowUp":
      case "ArrowDown": {
        event.preventDefault();

        let indexToFocus = 0;

        if (event.key === "ArrowUp") {
          if (focusedItemIndex === 0) {
            indexToFocus = childArray.length - 1;
          } else {
            indexToFocus = focusedItemIndex - 1;
          }
        }

        if (event.key === "ArrowDown") {
          if (focusedItemIndex === childArray.length - 1) {
            indexToFocus = 0;
          } else {
            indexToFocus = focusedItemIndex + 1;
          }
        }

        setFocusedItemIndex(indexToFocus);
        break;
      }
    }
  };

  return (
    <Select
      {...rest}
      onClick={toggle}
      onOpenChange={(isOpen) => onToggle?.(isOpen)}
      onSelect={(_, value) => {
        onSelect?.(value || "");
        onFilter?.("");
        setFilterValue("");
      }}
      maxMenuHeight={propertyToString(maxHeight)}
      popperProps={{ direction, width: propertyToString(width) }}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          id={toggleId}
          variant="typeahead"
          onClick={() => onToggle?.(true)}
          icon={toggleIcon}
          isDisabled={isDisabled}
          isExpanded={rest.isOpen}
          isFullWidth
          status={validated === "error" ? MenuToggleStatus.danger : undefined}
        >
          <TextInputGroup isPlain>
            <TextInputGroupMain
              placeholder={placeholderText}
              value={
                variant === SelectVariant.typeahead && selections
                  ? (selections as string)
                  : filterValue
              }
              onClick={toggle}
              onChange={(_, value) => {
                setFilterValue(value);
                onFilter?.(value);
              }}
              onKeyDown={(event) => onInputKeyDown(event)}
              autoComplete="off"
              innerRef={textInputRef}
              role="combobox"
              isExpanded={rest.isOpen}
              aria-controls="select-typeahead-listbox"
              aria-label={typeAheadAriaLabel}
            >
              {variant === SelectVariant.typeaheadMulti &&
                Array.isArray(selections) &&
                (chipGroupComponent ? (
                  chipGroupComponent
                ) : (
                  <LabelGroup {...chipGroupPropsRest}>
                    {selections
                      .splice(numChips || 0)
                      .map((selection, index: number) => (
                        <Label
                          variant="outline"
                          key={index}
                          onClose={(ev) => {
                            ev.stopPropagation();
                            onSelect?.(selection);
                          }}
                        >
                          {selection}
                        </Label>
                      ))}
                  </LabelGroup>
                ))}
            </TextInputGroupMain>
            <TextInputGroupUtilities>
              {!!filterValue && (
                <Button
                  icon={<TimesIcon aria-hidden />}
                  variant="plain"
                  onClick={() => {
                    onSelect?.("");
                    setFilterValue("");
                    onFilter?.("");
                    textInputRef?.current?.focus();
                  }}
                  aria-label="Clear input value"
                />
              )}
            </TextInputGroupUtilities>
          </TextInputGroup>
        </MenuToggle>
      )}
    >
      <SelectList>{children}</SelectList>
      {footer && <MenuFooter>{footer}</MenuFooter>}
    </Select>
  );
};
