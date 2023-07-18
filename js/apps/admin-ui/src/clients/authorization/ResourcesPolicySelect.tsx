import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type {
  Clients,
  PolicyQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/clients";
import {
  ButtonVariant,
  Chip,
  ChipGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  ControllerRenderProps,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { Link, useNavigate } from "react-router-dom";
import { adminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";
import { toPolicyDetails } from "../routes/PolicyDetails";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

type Type = "resources" | "policies";

type ResourcesPolicySelectProps = {
  name: Type;
  clientId: string;
  permissionId?: string;
  variant?: SelectVariant;
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
  const { realm } = useRealm();
  const { t } = useTranslation("clients");
  const navigate = useNavigate();

  const {
    control,
    formState: { errors, isDirty },
  } = useFormContext<PolicyRepresentation>();
  const [items, setItems] = useState<Policies[]>([]);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const [clickedPolicy, setClickedPolicy] = useState<Policies>();

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
      return (
        await Promise.all([
          adminClient.clients[functions.searchFunction](params),
          permissionId
            ? adminClient.clients[functions.fetchFunction]({
                id: clientId,
                permissionId,
              })
            : Promise.resolve([]),
        ])
      )
        .flat()
        .filter(
          (r): r is PolicyRepresentation | ResourceRepresentation =>
            typeof r !== "string",
        )
        .map(convert)
        .filter(
          ({ id }, index, self) =>
            index === self.findIndex(({ id: otherId }) => id === otherId),
        );
    },
    setItems,
    [search],
  );

  const [toggleUnsavedChangesDialog, UnsavedChangesConfirm] = useConfirmDialog({
    titleKey: t("unsavedChangesTitle"),
    messageKey: t("unsavedChangesConfirm"),
    continueButtonLabel: t("common:continue"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: () => navigate(to(clickedPolicy!)),
  });

  const to = (policy: Policies) =>
    toPolicyDetails({
      realm: realm,
      id: clientId,
      policyId: policy.id!,
      policyType: policy.type!,
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
          const policy = items.find(
            (permission) => permission.id === permissionId,
          );

          if (!policy) {
            return null;
          }

          return (
            <Chip
              key={policy.id}
              onClick={(event) => {
                event.stopPropagation();
                field.onChange(field.value?.filter((id) => id !== policy.id));
              }}
            >
              {policy.type ? (
                <Link
                  to={to(policy)}
                  onClick={(event) => {
                    if (isDirty) {
                      event.preventDefault();
                      event.stopPropagation();
                      setOpen(false);
                      setClickedPolicy(policy);
                      toggleUnsavedChangesDialog();
                    }
                  }}
                >
                  {policy.name}
                </Link>
              ) : (
                policy.name
              )}
            </Chip>
          );
        })}
      </ChipGroup>
    );
  };

  return (
    <>
      <UnsavedChangesConfirm />
      <Controller
        name={name}
        defaultValue={preSelected ? [preSelected] : []}
        control={control}
        rules={{ validate: (value) => !isRequired || value!.length > 0 }}
        render={({ field }) => (
          <Select
            toggleId={name}
            variant={variant}
            onToggle={setOpen}
            onFilter={(_, filter) => {
              setSearch(filter);
              return toSelectOptions();
            }}
            onClear={() => {
              field.onChange([]);
              setSearch("");
            }}
            selections={field.value}
            onSelect={(_, selectedValue) => {
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
            aria-labelledby={t(name)}
            isDisabled={!!preSelected}
            validated={errors[name] ? "error" : "default"}
            typeAheadAriaLabel={t(name)}
            chipGroupComponent={toChipGroupItems(field)}
          >
            {toSelectOptions()}
          </Select>
        )}
      />
    </>
  );
};
