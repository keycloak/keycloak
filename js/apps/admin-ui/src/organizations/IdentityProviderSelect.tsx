import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { IdentityProvidersQuery } from "@keycloak/keycloak-admin-client/lib/resources/identityProviders";
import { FormErrorText, HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { debounce } from "lodash-es";
import { useCallback, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { ComponentProps } from "../components/dynamic/components";
import { useFetch } from "../utils/useFetch";
import useToggle from "../utils/useToggle";

type IdentityProviderSelectProps = ComponentProps & {
  variant?: SelectVariant;
  isRequired?: boolean;
};

export const IdentityProviderSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isRequired,
  variant = SelectVariant.typeahead,
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

  const [open, toggleOpen] = useToggle();
  const [idps, setIdps] = useState<
    (IdentityProviderRepresentation | undefined)[]
  >([]);
  const [search, setSearch] = useState("");

  const debounceFn = useCallback(debounce(setSearch, 1000), []);

  useFetch(
    () => {
      const params: IdentityProvidersQuery = {
        max: 20,
      };
      if (search) {
        params.search = search;
      }

      return adminClient.identityProviders.find(params);
    },
    setIdps,
    [search],
  );

  const convert = (
    identityProviders: (IdentityProviderRepresentation | undefined)[],
  ) =>
    identityProviders
      .filter((i) => i !== undefined)
      .map((option) => (
        <SelectOption
          key={option!.alias}
          value={option!.alias}
          selected={values?.includes(option!.alias!)}
        >
          {option!.alias}
        </SelectOption>
      ));

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
        rules={{ required: isRequired }}
        render={({ field }) => (
          <Select
            toggleId={name!}
            variant={variant}
            placeholderText={t("selectIdentityProvider")}
            onToggle={toggleOpen}
            isOpen={open}
            selections={field.value}
            onFilter={(_, value) => {
              debounceFn(value);
              return convert(idps);
            }}
            menuAppendTo="parent"
            onSelect={(_, v) => {
              const option = v.toString();
              field.value.includes(option)
                ? field.onChange([])
                : field.onChange([option]);
              toggleOpen();
            }}
            aria-label={t(name!)}
            isDisabled={isDisabled}
          >
            {convert(idps)}
          </Select>
        )}
      />
      {errors[name!] && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
