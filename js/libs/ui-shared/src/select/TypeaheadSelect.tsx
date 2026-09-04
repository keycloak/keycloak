import {
  Button,
  Chip,
  ChipGroup,
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
  onClear,
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
  const [isFiltering, setIsFiltering] = useState(false);
  const [focusedItemIndex, setFocusedItemIndex] = useState<number>(0);
  const textInputRef = useRef<HTMLInputElement>();

  const childArray = Children.toArray(
    children,
  ) as React.ReactElement<SelectOptionProps>[];

  // Only filter while the user is actually typing, so that re-opening the menu
  // after a selection still lists every option.
  const visibleChildren =
    onFilter || !isFiltering || !filterValue
      ? childArray
      : childArray.filter((child) => {
          const { children: label, value } = child.props;
          const text = typeof label === "string" ? label : String(value ?? "");
          return text.toLowerCase().includes(filterValue.toLowerCase());
        });

  // The single typeahead shows the current selection whenever the user is not
  // editing, but their keystrokes must always win over it while they are.
  const inputValue =
    variant === SelectVariant.typeahead && !isFiltering && selections
      ? (selections as string)
      : filterValue;

  const stopFiltering = () => {
    setIsFiltering(false);
    setFilterValue("");
    setFocusedItemIndex(0);
    onFilter?.("");
  };

  const toggle = () => {
    onToggle(!rest.isOpen);
  };

  const onInputKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    const focusedItem = visibleChildren.at(focusedItemIndex);
    onToggle(true);

    switch (event.key) {
      case "Enter": {
        event.preventDefault();
        if (!focusedItem) break;

        onSelect?.(focusedItem.props.value);
        onToggle(false);
        stopFiltering();

        break;
      }
      case "Escape": {
        onToggle(false);
        stopFiltering();
        break;
      }
      case "ArrowUp":
      case "ArrowDown": {
        event.preventDefault();
        if (visibleChildren.length === 0) break;

        let indexToFocus = 0;

        if (event.key === "ArrowUp") {
          if (focusedItemIndex === 0) {
            indexToFocus = visibleChildren.length - 1;
          } else {
            indexToFocus = focusedItemIndex - 1;
          }
        }

        if (event.key === "ArrowDown") {
          if (focusedItemIndex === visibleChildren.length - 1) {
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
      onOpenChange={(isOpen) => {
        onToggle(isOpen);
        if (!isOpen) {
          stopFiltering();
        }
      }}
      onSelect={(_, value) => {
        onSelect?.(value || "");
        stopFiltering();
      }}
      maxMenuHeight={propertyToString(maxHeight)}
      popperProps={{ direction, width: propertyToString(width) }}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          id={toggleId}
          variant="typeahead"
          onClick={() => onToggle(true)}
          icon={toggleIcon}
          isDisabled={isDisabled}
          isExpanded={rest.isOpen}
          isFullWidth
          status={validated === "error" ? MenuToggleStatus.danger : undefined}
        >
          <TextInputGroup isPlain>
            <TextInputGroupMain
              placeholder={placeholderText}
              value={inputValue}
              onClick={toggle}
              onChange={(_, value) => {
                setIsFiltering(true);
                setFilterValue(value);
                setFocusedItemIndex(0);
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
                  <ChipGroup {...chipGroupProps}>
                    {selections.map((selection, index: number) => (
                      <Chip
                        key={index}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          onSelect?.(selection);
                        }}
                      >
                        {selection}
                      </Chip>
                    ))}
                  </ChipGroup>
                ))}
            </TextInputGroupMain>
            <TextInputGroupUtilities>
              {!!inputValue && (
                <Button
                  variant="plain"
                  onClick={() => {
                    // Consumers that track their own value need to reset it
                    // themselves: onSelect("") stores an empty entry rather
                    // than an empty selection.
                    if (onClear) {
                      onClear();
                    } else {
                      onSelect?.("");
                    }
                    stopFiltering();
                    textInputRef.current?.focus();
                  }}
                  aria-label="Clear input value"
                >
                  <TimesIcon aria-hidden />
                </Button>
              )}
            </TextInputGroupUtilities>
          </TextInputGroup>
        </MenuToggle>
      )}
    >
      <SelectList>{visibleChildren}</SelectList>
      {footer && <MenuFooter>{footer}</MenuFooter>}
    </Select>
  );
};
