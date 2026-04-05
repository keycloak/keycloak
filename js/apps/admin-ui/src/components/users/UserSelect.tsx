import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  FormErrorText,
  HelpItem,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
} from "@patternfly/react-core";
import { TimesIcon } from "@patternfly/react-icons";
import { debounce } from "lodash-es";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import useToggle from "../../utils/useToggle";
import type { ComponentProps } from "../dynamic/components";

type UserSelectVariant = "typeaheadMulti" | "typeahead";

type UserSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: UserSelectVariant;
  isRequired?: boolean;
};

export const UserSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isRequired,
  variant = "typeaheadMulti",
}: UserSelectProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const {
    control,
    getValues,
    formState: { errors },
  } = useFormContext();
  const values: string[] | undefined = getValues(name!);

  const [open, toggleOpen, setOpen] = useToggle();
  const [selectedUsers, setSelectedUsers] = useState<UserRepresentation[]>([]);
  const [searchedUsers, setSearchedUsers] = useState<UserRepresentation[]>([]);
  const [inputValue, setInputValue] = useState("");
  const [search, setSearch] = useState("");
  const textInputRef = useRef<HTMLInputElement>();

  const debounceFn = useCallback(debounce(setSearch, 500), []);

  useFetch(
    async () => {
      if (!values) {
        return [];
      }

      const foundUsers = await Promise.all(
        values.map((id) => adminClient.users.findOne({ id })),
      );

      return foundUsers.filter((user) => user !== undefined);
    },
    (users) => {
      setSelectedUsers(users);
      if (variant !== "typeaheadMulti") {
        setInputValue(users[0]?.username || "");
      }
    },
    [values],
  );

  useFetch(
    async () =>
      adminClient.users.find({
        username: search,
        max: 20,
      }),
    setSearchedUsers,
    [search],
  );

  useEffect(() => {
    if (!values || values.length === 0) {
      setSelectedUsers([]);
      setInputValue("");
    }
  }, [values]);

  const users = useMemo(
    () => [...selectedUsers, ...searchedUsers],
    [selectedUsers, searchedUsers],
  );

  const convert = (users: UserRepresentation[]) =>
    users.map((option) => (
      <SelectOption
        key={option.id}
        value={option.id}
        selected={values?.includes(option.id!)}
      >
        {option.username}
      </SelectOption>
    ));

  return (
    <FormGroup
      label={t(label!)}
      isRequired={isRequired}
      labelIcon={<HelpItem helpText={helpText!} fieldLabelId={t(label!)} />}
      fieldId={name!}
    >
      <Controller
        name={name!}
        defaultValue={defaultValue}
        control={control}
        rules={
          isRequired && variant === "typeaheadMulti"
            ? { validate: (value) => value.length > 0 }
            : { required: isRequired }
        }
        render={({ field }) => (
          <Select
            id={name!}
            onOpenChange={toggleOpen}
            toggle={(ref) => (
              <MenuToggle
                data-testid={name!}
                ref={ref}
                variant="typeahead"
                onClick={toggleOpen}
                isExpanded={open}
                isFullWidth
                status={errors[name!] ? "danger" : undefined}
              >
                <TextInputGroup isPlain>
                  <TextInputGroupMain
                    value={inputValue}
                    onClick={toggleOpen}
                    onChange={(_, value) => {
                      setOpen(true);
                      setInputValue(value);
                      debounceFn(value);
                    }}
                    autoComplete="off"
                    innerRef={textInputRef}
                    placeholderText={t("selectAUser")}
                    {...(field.value && {
                      "aria-activedescendant": field.value,
                    })}
                    role="combobox"
                    isExpanded={open}
                    aria-controls="select-create-typeahead-listbox"
                  >
                    {variant === "typeaheadMulti" &&
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
                                      (item: string) => item !== selection,
                                    ),
                                  );
                                }}
                              >
                                {
                                  users.find((u) => u?.id === selection)
                                    ?.username
                                }
                              </Chip>
                            ),
                          )}
                        </ChipGroup>
                      )}
                  </TextInputGroupMain>
                  <TextInputGroupUtilities>
                    {!!search && (
                      <Button
                        variant="plain"
                        onClick={() => {
                          setInputValue("");
                          setSearch("");
                          field.onChange([]);
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
            isOpen={open}
            selected={field.value}
            onSelect={(_, v) => {
              const option = v?.toString();
              if (variant !== "typeaheadMulti") {
                const removed = field.value.includes(option);

                if (removed) {
                  field.onChange([]);
                } else {
                  field.onChange([option]);
                }

                setInputValue(
                  removed
                    ? ""
                    : users.find((u) => u?.id === option)?.username || "",
                );
                setOpen(false);
              } else {
                const changedValue = field.value.find(
                  (v: string) => v === option,
                )
                  ? field.value.filter((v: string) => v !== option)
                  : [...field.value, option];
                field.onChange(changedValue);
              }
            }}
            aria-label={t(name!)}
          >
            <SelectList>{convert(searchedUsers)}</SelectList>
          </Select>
        )}
      />
      {errors[name!] && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
