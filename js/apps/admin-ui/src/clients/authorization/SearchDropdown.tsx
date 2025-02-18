import PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { SelectControl, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  Dropdown,
  Form,
  MenuToggle,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
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
};

type SearchDropdownProps = {
  resources?: UserRepresentation[];
  types?: PolicyRepresentation[];
  search: SearchForm;
  onSearch: (form: SearchForm) => void;
  type: "resource" | "policy" | "permission" | "adminPermission";
};

export const SearchDropdown = ({
  resources,
  types,
  search,
  onSearch,
  type,
}: SearchDropdownProps) => {
  const { t } = useTranslation();
  const form = useForm<SearchForm>({
    mode: "onChange",
    defaultValues: search,
  });

  const {
    reset,
    formState: { isDirty },
    handleSubmit,
  } = form;

  const [open, toggle] = useToggle();
  const [resourceScopes, setResourceScopes] = useState<string[]>([]);
  const selectedType = useWatch({ control: form.control, name: "type" });
  const [key, setKey] = useState(0);

  const submit = (form: SearchForm) => {
    toggle();
    onSearch(form);
  };

  useEffect(() => {
    const type = types?.find((item) => item.type === selectedType);
    setResourceScopes(type?.scopes || []);
  }, [selectedType, types]);

  useEffect(() => {
    reset(search);
    setKey((prevKey) => prevKey + 1);
  }, [search]);

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
          key={key}
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
                ...(type !== "adminPermission"
                  ? [{ key: "", value: t("allTypes") }]
                  : []),
                ...(Array.isArray(types)
                  ? types.map(({ type, name }) => ({
                      key: type!,
                      value: name! || type!,
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
                ...(resources || []).map(({ id, username }) => ({
                  key: id!,
                  value: username!,
                })),
              ]}
            />
          )}
          {type === "adminPermission" && (
            <SelectControl
              name={"scope"}
              label={t("authorizationScope")}
              controller={{
                defaultValue: "",
              }}
              options={[
                ...(resourceScopes || []).map((resourceScope) => ({
                  key: resourceScope!,
                  value: resourceScope!,
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
              onClick={() => {
                reset({});
                onSearch({});
              }}
            >
              {t("clear")}
            </Button>
          </ActionGroup>
        </Form>
      </FormProvider>
    </Dropdown>
  );
};
