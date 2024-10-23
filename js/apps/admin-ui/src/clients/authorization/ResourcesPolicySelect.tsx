import PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type {
  Clients,
  PolicyQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/clients";
import {
  KeycloakSelect,
  SelectVariant,
  useFetch,
  Variant,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  SelectOption,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  ControllerRenderProps,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import { toCreatePolicy } from "../routes/NewPolicy";
import { toPolicyDetails } from "../routes/PolicyDetails";
import { toResourceDetails } from "../routes/Resource";
import { NewPolicyDialog } from "./NewPolicyDialog";

type Type = "resources" | "policies";

type ResourcesPolicySelectProps = {
  name: Type;
  clientId: string;
  permissionId?: string;
  variant?: Variant;
  preSelected?: string;
  isRequired?: boolean;
};

type Policies = {
  id?: string;
  name?: string;
  type?: string;
};

type TypeMapping = {
  [key in Type]: {
    searchFunction: keyof Pick<Clients, "listPolicies" | "listResources">;
    fetchFunction: keyof Pick<
      Clients,
      "getAssociatedPolicies" | "getAssociatedResources"
    >;
  };
};

const typeMapping: TypeMapping = {
  resources: {
    searchFunction: "listResources",
    fetchFunction: "getAssociatedResources",
  },
  policies: {
    searchFunction: "listPolicies",
    fetchFunction: "getAssociatedPolicies",
  },
};

export const ResourcesPolicySelect = ({
  name,
  clientId,
  permissionId,
  variant = SelectVariant.typeaheadMulti,
  preSelected,
  isRequired = false,
}: ResourcesPolicySelectProps) => {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { t } = useTranslation();
  const navigate = useNavigate();

  const {
    control,
    formState: { errors, isDirty },
  } = useFormContext<PolicyRepresentation>();
  const [items, setItems] = useState<Policies[]>([]);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const [createPolicyDialog, toggleCreatePolicyDialog] = useToggle();
  const [policyProviders, setPolicyProviders] =
    useState<PolicyProviderRepresentation[]>();
  const [onUnsavedChangesConfirm, setOnUnsavedChangesConfirm] =
    useState<() => void>();

  const functions = typeMapping[name];

  const convert = (
    p: PolicyRepresentation | ResourceRepresentation,
  ): Policies => ({
    id: "_id" in p ? p._id : "id" in p ? p.id : undefined,
    name: p.name,
    type: p.type,
  });

  useFetch(
    async () => {
      const params: PolicyQuery = Object.assign(
        { id: clientId, first: 0, max: 10, permission: "false" },
        search === "" ? null : { name: search },
      );
      return await Promise.all([
        adminClient.clients.listPolicyProviders({ id: clientId }),
        adminClient.clients[functions.searchFunction](params),
        permissionId
          ? adminClient.clients[functions.fetchFunction]({
              id: clientId,
              permissionId,
            })
          : Promise.resolve([]),
        preSelected && name === "resources"
          ? adminClient.clients.getResource({
              id: clientId,
              resourceId: preSelected,
            })
          : Promise.resolve([]),
      ]);
    },
    ([providers, ...policies]) => {
      setPolicyProviders(
        providers.filter((p) => p.type !== "resource" && p.type !== "scope"),
      );
      setItems(
        policies
          .flat()
          .filter(
            (r): r is PolicyRepresentation | ResourceRepresentation =>
              typeof r !== "string",
          )
          .map(convert)
          .filter(
            ({ id }, index, self) =>
              index === self.findIndex(({ id: otherId }) => id === otherId),
          ),
      );
    },
    [search],
  );

  const [toggleUnsavedChangesDialog, UnsavedChangesConfirm] = useConfirmDialog({
    titleKey: t("unsavedChangesTitle"),
    messageKey: t("unsavedChangesConfirm"),
    continueButtonLabel: t("continue"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: () => onUnsavedChangesConfirm?.(),
  });

  const to = (policy: Policies) =>
    name === "policies"
      ? toPolicyDetails({
          realm: realm,
          id: clientId,
          policyId: policy.id!,
          policyType: policy.type!,
        })
      : toResourceDetails({
          realm,
          id: clientId,
          resourceId: policy.id!,
        });

  const toSelectOptions = () =>
    items.map((p) => (
      <SelectOption key={p.id} value={p.id}>
        {p.name}
      </SelectOption>
    ));

  const toChipGroupItems = (
    field: ControllerRenderProps<PolicyRepresentation, Type>,
  ) => {
    return (
      <ChipGroup>
        {field.value?.map((permissionId) => {
          const item = items.find(
            (permission) => permission.id === permissionId,
          );

          if (!item) return;

          const route = to(item);
          return (
            <Chip
              key={item.id}
              onClick={() => {
                field.onChange(field.value?.filter((id) => id !== item.id));
              }}
            >
              <Link
                to={route}
                onClick={(event) => {
                  if (isDirty) {
                    event.preventDefault();
                    setOnUnsavedChangesConfirm(() => () => navigate(route));
                    toggleUnsavedChangesDialog();
                  }
                }}
              >
                {item.name}
              </Link>
            </Chip>
          );
        })}
      </ChipGroup>
    );
  };

  return (
    <>
      <UnsavedChangesConfirm />
      {createPolicyDialog && (
        <NewPolicyDialog
          policyProviders={policyProviders}
          onSelect={(p) => {
            navigate(
              toCreatePolicy({ id: clientId, realm, policyType: p.type! }),
            );
          }}
          toggleDialog={toggleCreatePolicyDialog}
        />
      )}
      <Controller
        name={name}
        defaultValue={preSelected ? [preSelected] : []}
        control={control}
        rules={{ validate: (value) => !isRequired || value!.length > 0 }}
        render={({ field }) => (
          <KeycloakSelect
            toggleId={name}
            variant={variant}
            onToggle={(val) => setOpen(val)}
            onFilter={(filter) => {
              setSearch(filter);
              return toSelectOptions();
            }}
            onClear={() => {
              field.onChange([]);
              setSearch("");
            }}
            selections={field.value}
            onSelect={(selectedValue) => {
              const option = selectedValue.toString();
              if (variant === SelectVariant.typeaheadMulti) {
                const changedValue = field.value?.find(
                  (p: string) => p === option,
                )
                  ? field.value.filter((p: string) => p !== option)
                  : [...field.value!, option];
                field.onChange(changedValue);
              } else {
                field.onChange([option]);
              }

              setSearch("");
            }}
            isOpen={open}
            aria-label={t(name)}
            isDisabled={!!preSelected}
            validated={errors[name] ? "error" : "default"}
            typeAheadAriaLabel={t(name)}
            chipGroupComponent={toChipGroupItems(field)}
            footer={
              name === "policies" ? (
                <Button
                  variant="link"
                  isInline
                  onClick={() => {
                    if (isDirty) {
                      setOpen(false);
                      setOnUnsavedChangesConfirm(
                        () => toggleCreatePolicyDialog,
                      );
                      toggleUnsavedChangesDialog();
                    } else {
                      toggleCreatePolicyDialog();
                    }
                  }}
                >
                  {t("createPolicy")}
                </Button>
              ) : undefined
            }
          >
            {toSelectOptions()}
          </KeycloakSelect>
        )}
      />
    </>
  );
};
