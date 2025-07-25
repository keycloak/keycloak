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
import { getRuleValue } from "../../utils/getRuleValue";
import { FormLabel } from "../FormLabel";
import {
  OptionType,
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
  selectedOptions = [],
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
  const [selectedOptionsState, setSelectedOptions] = useState<
    SelectControlOption[]
  >([]);
  const textInputRef = useRef<HTMLInputElement>();
  const required = getRuleValue(controller.rules?.required) === true;
  const isTypeaheadMulti = variant === SelectVariant.typeaheadMulti;

  const combinedOptions = useMemo(
    () =>
      [
        ...options.filter(
          (o) => !selectedOptions.map((o) => getValue(o)).includes(getValue(o)),
        ),
        ...selectedOptions,
      ] as OptionType,
    [selectedOptions, options],
  );

  const filteredOptions = combinedOptions.filter((option) =>
    getValue(option).toLowerCase().startsWith(filterValue.toLowerCase()),
  );

  const updateValue = (
    option: string | string[],
    field: ControllerRenderProps<FieldValues, string>,
  ) => {
    if (field.value.includes(option)) {
      field.onChange(field.value.filter((item: string) => item !== option));
      if (isSelectBasedOptions(options)) {
        setSelectedOptions(
          selectedOptionsState.filter((item) => item.key !== option),
        );
      }
    } else {
      field.onChange([...field.value, option]);
      if (isSelectBasedOptions(combinedOptions)) {
        setSelectedOptions([
          ...selectedOptionsState,
          combinedOptions.find((o) => o.key === option)!,
        ]);
      }
    }
  };

  const onInputKeyDown = (
    event: React.KeyboardEvent<HTMLDivElement>,
    field: ControllerRenderProps<FieldValues, string>,
  ) => {
    const focusedItem = filteredOptions[focusedItemIndex];
    setOpen(true);

    switch (event.key) {
      case "Enter": {
        event.preventDefault();

        if (!isTypeaheadMulti) {
          setFilterValue(getValue(focusedItem));
        } else {
          setFilterValue("");
        }

        updateValue(key(focusedItem), field);

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
      id={id}
      name={name}
      label={label}
      isRequired={required}
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
            onOpenChange={() => setOpen(false)}
            selected={
              isSelectBasedOptions(combinedOptions)
                ? combinedOptions
                    .filter((o) =>
                      Array.isArray(field.value)
                        ? field.value.includes(o.key)
                        : field.value === o.key,
                    )
                    .map((o) => o.value)
                : field.value
            }
            shouldFocusFirstItemOnOpen={false}
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                id={id || name}
                variant="typeahead"
                onClick={() => {
                  setOpen(!open);
                  textInputRef.current?.focus();
                }}
                isExpanded={open}
                isFullWidth
                status={get(errors, name) ? MenuToggleStatus.danger : undefined}
              >
                <TextInputGroup isPlain>
                  <TextInputGroupMain
                    placeholder={placeholderText}
                    value={
                      variant === SelectVariant.typeahead && field.value
                        ? isSelectBasedOptions(combinedOptions)
                          ? combinedOptions.find(
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
                                {isSelectBasedOptions(combinedOptions)
                                  ? [
                                      ...combinedOptions,
                                      ...selectedOptionsState,
                                    ].find((o) => selection === o.key)?.value
                                  : getValue(selection)}
                              </Chip>
                            ),
                          )}
                        </ChipGroup>
                      )}
                  </TextInputGroupMain>
                  <TextInputGroupUtilities>
                    {(!!filterValue || field.value) && (
                      <Button
                        variant="plain"
                        onClick={() => {
                          setFilterValue("");
                          field.onChange(isTypeaheadMulti ? [] : "");
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
              if (isTypeaheadMulti && Array.isArray(field.value)) {
                setFilterValue("");
                updateValue(option || "", field);
              } else {
                field.onChange(Array.isArray(field.value) ? [option] : option);
                setOpen(false);
              }
            }}
            isOpen={open}
          >
            <SelectList>
              {filteredOptions.map((option, index) => (
                <SelectOption
                  key={key(option)}
                  value={key(option)}
                  isFocused={focusedItemIndex === index}
                  isActive={field.value.includes(getValue(option))}
                >
                  {getValue(option)}
                </SelectOption>
              ))}
            </SelectList>
          </Select>
        )}
      />
    </FormLabel>
  );
};
