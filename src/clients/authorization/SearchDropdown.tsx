import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  Dropdown,
  DropdownToggle,
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";

import type PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import useToggle from "../../utils/useToggle";

import "./search-dropdown.css";

type SearchDropdownProps = {
  types?: PolicyProviderRepresentation[];
};

export const SearchDropdown = ({ types }: SearchDropdownProps) => {
  const { t } = useTranslation("clients");
  const { register, control } = useForm();

  const [open, toggle] = useToggle();
  const [typeOpen, toggleType] = useToggle();

  return (
    <Dropdown
      data-testid="searchdropdown_dorpdown"
      className="pf-u-ml-md"
      toggle={
        <DropdownToggle
          onToggle={toggle}
          className="keycloak__client_authentication__searchdropdown"
        >
          {t("searchForPermission")}
        </DropdownToggle>
      }
      isOpen={open}
    >
      <Form
        isHorizontal
        className="keycloak__client_authentication__searchdropdown_form"
      >
        <FormGroup label={t("common:name")} fieldId="name">
          <TextInput
            ref={register}
            type="text"
            id="name"
            name="name"
            data-testid="searchdropdown_name"
          />
        </FormGroup>
        <FormGroup label={t("common:type")} fieldId="type">
          <Controller
            name="type"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="type"
                onToggle={toggleType}
                onSelect={(event, value) => {
                  event.stopPropagation();
                  onChange(value);
                  toggleType();
                }}
                selections={value.name}
                variant={SelectVariant.single}
                aria-label={t("common:type")}
                isOpen={typeOpen}
              >
                {types?.map((type) => (
                  <SelectOption
                    selected={type.type === value.type}
                    key={type.type}
                    value={type}
                  >
                    {type.name}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
      </Form>
    </Dropdown>
  );
};
