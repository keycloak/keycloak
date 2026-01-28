import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { IdentityProvidersQuery } from "@keycloak/keycloak-admin-client/lib/resources/identityProviders";
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
import { useCallback, useRef, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { ComponentProps } from "../components/dynamic/components";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import useToggle from "../utils/useToggle";

type IdentityProviderSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: "typeaheadMulti" | "typeahead";
  isRequired?: boolean;
};

export const IdentityProviderSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isRequired,
  variant = "typeahead",
  isDisabled,
}: IdentityProviderSelectProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const {
    control,
    getValues,
    formState: { errors },
  } = useFormContext();
  const values: string[] | undefined = getValues(name!);

  const [open, toggleOpen, setOpen] = useToggle();
  const [inputValue, setInputValue] = useState("");
  const textInputRef = useRef<HTMLInputElement>();
  const [idps, setIdps] = useState<
    (IdentityProviderRepresentation | undefined)[]
  >([]);
  const [search, setSearch] = useState("");

  const debounceFn = useCallback(debounce(setSearch, 1000), []);

  useFetch(
    async () => {
      const params: IdentityProvidersQuery = {
        max: 20,
        realmOnly: true,
      };
      if (search) {
        params.search = search;
      }

      return await adminClient.identityProviders.find(params);
    },
    setIdps,
    [search],
  );

  const convert = (
    identityProviders: (IdentityProviderRepresentation | undefined)[],
  ) => {
    const options = identityProviders.map((option) => (
      <SelectOption
        key={option!.alias}
        value={option}
        selected={values?.includes(option!.alias!)}
      >
        {option!.alias}
      </SelectOption>
    ));
    if (options.length === 0) {
      return <SelectOption value="">{t("noResultsFound")}</SelectOption>;
    }
    return options;
  };

  if (!idps) {
    return <KeycloakSpinner />;
  }
  return (
    <FormGroup
      label={t(label!)}
      isRequired={isRequired}
      labelIcon={
        helpText ? (
          <HelpItem helpText={helpText!} fieldLabelId={label!} />
        ) : undefined
      }
      fieldId={name!}
    >
      <Controller
        name={name!}
        defaultValue={defaultValue}
        control={control}
        rules={{
          validate: (value: string[]) =>
            isRequired && value.filter((i) => i !== undefined).length === 0
              ? t("required")
              : undefined,
        }}
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
                isDisabled={isDisabled}
                status={errors[name!] ? "danger" : undefined}
              >
                <TextInputGroup isPlain>
                  <TextInputGroupMain
                    value={inputValue || field.value}
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
                                {selection}
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
                        aria-label={t("clear")}
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
              const idp = v as IdentityProviderRepresentation;
              const option = idp.alias!;
              if (variant !== "typeaheadMulti") {
                const removed = field.value.includes(option);

                if (removed) {
                  field.onChange([]);
                } else {
                  field.onChange([option]);
                }

                setInputValue(removed ? "" : option || "");
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
            <SelectList>{convert(idps)}</SelectList>
          </Select>
        )}
      />
      {errors[name!] && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
