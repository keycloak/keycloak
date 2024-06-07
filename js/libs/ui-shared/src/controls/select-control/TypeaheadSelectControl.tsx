import {
  Button,
  Chip,
  ChipGroup,
  MenuToggle,
  MenuToggleStatus,
  Select,
  SelectList,
  SelectOption,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
} from "@patternfly/react-core";
import { TimesIcon } from "@patternfly/react-icons";
import { get } from "lodash-es";
import { useMemo, useRef, useState } from "react";
import {
  Controller,
  ControllerRenderProps,
  FieldPath,
  FieldValues,
  useFormContext,
} from "react-hook-form";
import { FormLabel } from "../FormLabel";
import {
  SelectControlOption,
  SelectControlProps,
  SelectVariant,
  isSelectBasedOptions,
  isString,
  key,
} from "./SelectControl";

const getValue = (option: SelectControlOption | string) =>
  isString(option) ? option : option.value;

export const TypeaheadSelectControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>({
  id,
  name,
  label,
  options,
  controller,
  labelIcon,
  placeholderText,
  onFilter,
  variant,
  ...rest
}: SelectControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  const [filterValue, setFilterValue] = useState("");
  const [focusedItemIndex, setFocusedItemIndex] = useState<number>(0);
  const textInputRef = useRef<HTMLInputElement>();

  const filteredOptions = options.filter((option) =>
    getValue(option).toLowerCase().startsWith(filterValue.toLowerCase()),
  );

  const convert = useMemo(
    () =>
      filteredOptions.map((option, index) => (
        <SelectOption
          key={key(option)}
          value={key(option)}
          isFocused={focusedItemIndex === index}
        >
          {getValue(option)}
        </SelectOption>
      )),
    [focusedItemIndex, filteredOptions],
  );

  const onInputKeyDown = (
    event: React.KeyboardEvent<HTMLDivElement>,
    field: ControllerRenderProps<FieldValues, string>,
  ) => {
    const focusedItem = filteredOptions[focusedItemIndex];
    setOpen(true);

    switch (event.key) {
      case "Enter": {
        event.preventDefault();

        if (variant !== SelectVariant.typeaheadMulti) {
          setFilterValue(getValue(focusedItem));
        } else {
          setFilterValue("");
        }

        field.onChange(
          Array.isArray(field.value)
            ? [...field.value, key(focusedItem)]
            : key(focusedItem),
        );
        setOpen(false);
        setFocusedItemIndex(0);

        break;
      }
      case "Tab":
      case "Escape": {
        setOpen(false);
        field.onChange(undefined);
        break;
      }
      case "Backspace": {
        if (variant === SelectVariant.typeahead) {
          field.onChange("");
        }
        break;
      }
      case "ArrowUp":
      case "ArrowDown": {
        event.preventDefault();

        let indexToFocus = 0;

        if (event.key === "ArrowUp") {
          if (focusedItemIndex === 0) {
            indexToFocus = options.length - 1;
          } else {
            indexToFocus = focusedItemIndex - 1;
          }
        }

        if (event.key === "ArrowDown") {
          if (focusedItemIndex === options.length - 1) {
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
    <FormLabel
      name={name}
      label={label}
      isRequired={!!controller.rules?.required}
      error={get(errors, name)}
      labelIcon={labelIcon}
    >
      <Controller
        {...controller}
        name={name}
        control={control}
        render={({ field }) => (
          <Select
            {...rest}
            onClick={() => setOpen(!open)}
            onOpenChange={() => setOpen(false)}
            selected={
              isSelectBasedOptions(options)
                ? options
                    .filter((o) =>
                      Array.isArray(field.value)
                        ? field.value.includes(o.key)
                        : field.value === o.key,
                    )
                    .map((o) => o.value)
                : field.value
            }
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                id={id || name.slice(name.lastIndexOf(".") + 1)}
                variant="typeahead"
                onClick={() => setOpen(!open)}
                isExpanded={open}
                isFullWidth
                status={get(errors, name) ? MenuToggleStatus.danger : undefined}
              >
                <TextInputGroup isPlain>
                  <TextInputGroupMain
                    placeholder={placeholderText}
                    value={
                      variant === SelectVariant.typeahead && field.value
                        ? isSelectBasedOptions(options)
                          ? options.find(
                              (o) =>
                                o.key ===
                                (Array.isArray(field.value)
                                  ? field.value[0]
                                  : field.value),
                            )?.value
                          : field.value
                        : filterValue
                    }
                    onClick={() => setOpen(!open)}
                    onChange={(_, value) => {
                      setFilterValue(value);
                      onFilter?.(value);
                    }}
                    onKeyDown={(event) => onInputKeyDown(event, field)}
                    autoComplete="off"
                    innerRef={textInputRef}
                    role="combobox"
                    isExpanded={open}
                    aria-controls="select-typeahead-listbox"
                  >
                    {variant === SelectVariant.typeaheadMulti &&
                      Array.isArray(field.value) && (
                        <ChipGroup aria-label="Current selections">
                          {field.value.map(
                            (selection: string, index: number) => (
                              <Chip
                                key={index}
                                onClick={(ev) => {
                                  ev.stopPropagation();
                                  field.onChange(
                                    field.value.filter(
                                      (item: string) => item !== key(selection),
                                    ),
                                  );
                                }}
                              >
                                {isSelectBasedOptions(options)
                                  ? options.find((o) => selection === o.key)
                                      ?.value
                                  : getValue(selection)}
                              </Chip>
                            ),
                          )}
                        </ChipGroup>
                      )}
                  </TextInputGroupMain>
                  <TextInputGroupUtilities>
                    {!!filterValue && (
                      <Button
                        variant="plain"
                        onClick={() => {
                          field.onChange(undefined);
                          setFilterValue("");
                          textInputRef?.current?.focus();
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
            onSelect={(event, v) => {
              event?.stopPropagation();
              const option = v?.toString();
              if (
                variant === SelectVariant.typeaheadMulti &&
                Array.isArray(field.value)
              ) {
                if (field.value.includes(option)) {
                  field.onChange(
                    field.value.filter((item: string) => item !== option),
                  );
                } else {
                  field.onChange([...field.value, option]);
                }
              } else {
                field.onChange(Array.isArray(field.value) ? [option] : option);
                setOpen(false);
              }
            }}
            isOpen={open}
          >
            <SelectList>{convert}</SelectList>
          </Select>
        )}
      />
    </FormLabel>
  );
};
