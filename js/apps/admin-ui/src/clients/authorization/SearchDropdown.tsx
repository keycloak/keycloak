import type PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { SelectControl, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  Dropdown,
  Form,
  MenuToggle,
} from "@patternfly/react-core";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import useToggle from "../../utils/useToggle";

import "./search-dropdown.css";

export type SearchForm = {
  name?: string;
  resource?: string;
  scope?: string;
  type?: string;
  uri?: string;
  owner?: string;
};

type SearchDropdownProps = {
  types?: PolicyProviderRepresentation[] | PolicyProviderRepresentation[];
  search: SearchForm;
  onSearch: (form: SearchForm) => void;
  type: "resource" | "policy" | "permission";
};

export const SearchDropdown = ({
  types,
  search,
  onSearch,
  type,
}: SearchDropdownProps) => {
  const { t } = useTranslation();
  const form = useForm<SearchForm>({ mode: "onChange" });
  const {
    reset,
    formState: { isDirty },
    handleSubmit,
  } = form;

  const [open, toggle] = useToggle();

  const submit = (form: SearchForm) => {
    toggle();
    onSearch(form);
  };

  useEffect(() => reset(search), [search]);

  return (
    <Dropdown
      onOpenChange={toggle}
      toggle={(ref) => (
        <MenuToggle
          data-testid="searchdropdown_dorpdown"
          ref={ref}
          onClick={toggle}
          className="keycloak__client_authentication__searchdropdown"
        >
          {type === "resource" && t("searchClientAuthorizationResource")}
          {type === "policy" && t("searchClientAuthorizationPolicy")}
          {type === "permission" && t("searchClientAuthorizationPermission")}
        </MenuToggle>
      )}
      isOpen={open}
    >
      <FormProvider {...form}>
        <Form
          isHorizontal
          className="keycloak__client_authentication__searchdropdown_form"
          onSubmit={handleSubmit(submit)}
        >
          <TextControl name="name" label={t("name")} />
          {type === "resource" && (
            <>
              <TextControl name="type" label={t("type")} />
              <TextControl name="uris" label={t("uris")} />
              <TextControl name="owner" label={t("owner")} />
            </>
          )}
          {type !== "resource" && type !== "policy" && (
            <TextControl name="resource" label={t("resource")} />
          )}
          {type !== "policy" && <TextControl name="scope" label={t("scope")} />}
          {type !== "resource" && (
            <SelectControl
              name="type"
              label={t("type")}
              controller={{
                defaultValue: "",
              }}
              options={[
                { key: "", value: t("allTypes") },
                ...(types || []).map(({ type, name }) => ({
                  key: type!,
                  value: name!,
                })),
              ]}
            />
          )}
          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="search-btn"
              isDisabled={!isDirty}
            >
              {t("search")}
            </Button>
            <Button
              variant="link"
              data-testid="revert-btn"
              onClick={() => onSearch({})}
            >
              {t("clear")}
            </Button>
          </ActionGroup>
        </Form>
      </FormProvider>
    </Dropdown>
  );
};
