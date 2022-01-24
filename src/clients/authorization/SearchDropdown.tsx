import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  ActionGroup,
  Button,
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

export type SearchForm = {
  name?: string;
  resource?: string;
  scope?: string;
  type?: string;
};

type SearchDropdownProps = {
  types?: PolicyProviderRepresentation[];
  onSearch: (form: SearchForm) => void;
};

export const SearchDropdown = ({ types, onSearch }: SearchDropdownProps) => {
  const { t } = useTranslation("clients");
  const {
    register,
    control,
    formState: { isDirty },
    handleSubmit,
  } = useForm<SearchForm>({ mode: "onChange" });

  const [open, toggle] = useToggle();
  const [typeOpen, toggleType] = useToggle();

  const submit = (form: SearchForm) => {
    toggle();
    onSearch(form);
  };

  const typeOptions = (value: string) => [
    <SelectOption key="empty" value="">
      {t("allTypes")}
    </SelectOption>,
    ...(types || []).map((type) => (
      <SelectOption
        selected={type.type === value}
        key={type.type}
        value={type.type}
      >
        {type.name}
      </SelectOption>
    )),
  ];

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
        onSubmit={handleSubmit(submit)}
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
        <FormGroup label={t("resource")} fieldId="resource">
          <TextInput
            ref={register}
            type="text"
            id="resource"
            name="resource"
            data-testid="searchdropdown_resource"
          />
        </FormGroup>
        <FormGroup label={t("scope")} fieldId="scope">
          <TextInput
            ref={register}
            type="text"
            id="scope"
            name="scope"
            data-testid="searchdropdown_scope"
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
                selections={value || t("allTypes")}
                variant={SelectVariant.single}
                aria-label={t("common:type")}
                isOpen={typeOpen}
              >
                {typeOptions(value)}
              </Select>
            )}
          />
        </FormGroup>
        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="search-btn"
            isDisabled={!isDirty}
          >
            {t("common:search")}
          </Button>
          <Button
            variant="link"
            data-testid="revert-btn"
            onClick={() => onSearch({})}
          >
            {t("common:clear")}
          </Button>
        </ActionGroup>
      </Form>
    </Dropdown>
  );
};
