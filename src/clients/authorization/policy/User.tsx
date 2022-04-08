import React, { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import {
  SelectOption,
  FormGroup,
  Select,
  SelectVariant,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type { UserQuery } from "@keycloak/keycloak-admin-client/lib/resources/users";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { useTranslation } from "react-i18next";

export const User = () => {
  const { t } = useTranslation("clients");
  const {
    control,
    getValues,
    formState: { errors },
  } = useFormContext();
  const values: string[] | undefined = getValues("users");

  const [open, setOpen] = useState(false);
  const [users, setUsers] = useState<UserRepresentation[]>([]);
  const [search, setSearch] = useState("");

  const adminClient = useAdminClient();

  useFetch(
    async () => {
      const params: UserQuery = {
        max: 20,
      };
      if (search) {
        params.name = search;
      }

      if (values?.length && !search) {
        return await Promise.all(
          values.map(
            (id: string) =>
              adminClient.users.findOne({ id }) as UserRepresentation
          )
        );
      }
      return await adminClient.users.find(params);
    },
    setUsers,
    [search]
  );

  const convert = (clients: UserRepresentation[]) =>
    clients.map((option) => (
      <SelectOption
        key={option.id!}
        value={option.id}
        selected={values?.includes(option.id!)}
      >
        {option.username}
      </SelectOption>
    ));

  return (
    <FormGroup
      label={t("users")}
      labelIcon={
        <HelpItem
          helpText="clients-help:policyUsers"
          fieldLabelId="clients:users"
        />
      }
      fieldId="users"
      helperTextInvalid={t("common:required")}
      validated={errors.users ? "error" : "default"}
      isRequired
    >
      <Controller
        name="users"
        defaultValue={[]}
        control={control}
        rules={{
          validate: (value) => value.length > 0,
        }}
        render={({ onChange, value }) => (
          <Select
            toggleId="users"
            variant={SelectVariant.typeaheadMulti}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={value}
            onFilter={(_, value) => {
              setSearch(value);
              return convert(users);
            }}
            onSelect={(_, v) => {
              const option = v.toString();
              if (value.includes(option)) {
                onChange(value.filter((item: string) => item !== option));
              } else {
                onChange([...value, option]);
              }
              setOpen(false);
            }}
            aria-label={t("users")}
          >
            {convert(users)}
          </Select>
        )}
      />
    </FormGroup>
  );
};
