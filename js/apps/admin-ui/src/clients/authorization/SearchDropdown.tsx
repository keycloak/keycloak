import type PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
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
  resourceType?: string;
  policy?: string;
};

type SearchDropdownProps = {
  policies?: PolicyRepresentation[];
  types?: PolicyProviderRepresentation[] | PolicyProviderRepresentation[];
  resources?: ResourceRepresentation[];
  scopes?: ScopeRepresentation[];
  search: SearchForm;
  onSearch: (form: SearchForm) => void;
  type: "resource" | "policy" | "permission" | "adminPermission";
};

export const SearchDropdown = ({
  policies,
  types,
  resources,
  scopes,
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
          {(type === "permission" || type === "adminPermission") &&
            t("searchClientAuthorizationPermission")}
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
          {type !== "resource" &&
            type !== "policy" &&
            type !== "adminPermission" && (
              <TextControl name="resource" label={t("resource")} />
            )}
          {type !== "policy" && type !== "adminPermission" && (
            <TextControl name="scope" label={t("scope")} />
          )}
          {type !== "resource" && (
            <SelectControl
              name={type !== "adminPermission" ? "type" : "resourceType"}
              label={type !== "adminPermission" ? t("type") : t("resourceType")}
              controller={{
                defaultValue: "",
              }}
              options={[
                { key: "", value: t("allTypes") },
                ...(Array.isArray(types)
                  ? types.map(({ type, name }) => ({
                      key: type!,
                      value: name!,
                    }))
                  : []),
              ]}
            />
          )}
          {type === "adminPermission" && (
            <SelectControl
              name={"resource"}
              label={t("resource")}
              controller={{
                defaultValue: "",
              }}
              options={[
                ...(resources || []).map(({ type, name }) => ({
                  key: type!,
                  value: name!,
                })),
              ]}
            />
          )}
          {type === "adminPermission" && (
            <SelectControl
              name={"authorizationScope"}
              label={t("authorizationScope")}
              controller={{
                defaultValue: "",
              }}
              options={[
                ...(scopes || []).map(({ name }) => ({
                  key: name!,
                  value: name!,
                })),
              ]}
            />
          )}
          {type === "adminPermission" && (
            <SelectControl
              name={"policy"}
              label={t("policy")}
              controller={{
                defaultValue: "",
              }}
              options={[
                ...(policies || []).map(({ type, name }) => ({
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
