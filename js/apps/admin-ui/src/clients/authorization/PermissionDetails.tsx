import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import { DecisionStrategy } from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  Radio,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { toUpperCase } from "../../util";
import { useParams } from "../../utils/useParams";
import { toAuthorizationTab } from "../routes/AuthenticationTab";
import type { NewPermissionParams } from "../routes/NewPermission";
import {
  PermissionDetailsParams,
  toPermissionDetails,
} from "../routes/PermissionDetails";
import { ResourcesPolicySelect } from "./ResourcesPolicySelect";
import { ScopeSelect } from "./ScopeSelect";

type FormFields = PolicyRepresentation & {
  resourceType: string;
};

export default function PermissionDetails() {
  const { t } = useTranslation("clients");

  const form = useForm<FormFields>({
    mode: "onChange",
  });
  const {
    register,
    control,
    reset,
    formState: { errors },
    handleSubmit,
  } = form;

  const navigate = useNavigate();
  const { id, realm, permissionType, permissionId, selectedId } = useParams<
    NewPermissionParams & PermissionDetailsParams
  >();

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [permission, setPermission] = useState<PolicyRepresentation>();
  const [applyToResourceTypeFlag, setApplyToResourceTypeFlag] = useState(false);

  useFetch(
    async () => {
      if (!permissionId) {
        return {};
      }
      const [permission, resources, policies, scopes] = await Promise.all([
        adminClient.clients.findOnePermission({
          id,
          type: permissionType,
          permissionId,
        }),
        adminClient.clients.getAssociatedResources({
          id,
          permissionId,
        }),
        adminClient.clients.getAssociatedPolicies({
          id,
          permissionId,
        }),
        adminClient.clients.getAssociatedScopes({
          id,
          permissionId,
        }),
      ]);

      if (!permission) {
        throw new Error(t("common:notFound"));
      }

      return {
        permission,
        resources: resources.map((r) => r._id),
        policies: policies.map((p) => p.id!),
        scopes: scopes.map((s) => s.id!),
      };
    },
    ({ permission, resources, policies, scopes }) => {
      reset({ ...permission, resources, policies, scopes });
      if (permission && "resourceType" in permission) {
        setApplyToResourceTypeFlag(
          !!(permission as { resourceType: string }).resourceType
        );
      }
      setPermission({ ...permission, resources, policies });
    },
    []
  );

  const save = async (permission: PolicyRepresentation) => {
    try {
      if (permissionId) {
        await adminClient.clients.updatePermission(
          { id, type: permissionType, permissionId },
          permission
        );
      } else {
        const result = await adminClient.clients.createPermission(
          { id, type: permissionType },
          permission
        );
        navigate(
          toPermissionDetails({
            realm,
            id,
            permissionType,
            permissionId: result.id!,
          })
        );
      }
      addAlert(
        t((permissionId ? "update" : "create") + "PermissionSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("clients:permissionSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:deletePermission",
    messageKey: t("deletePermissionConfirm", {
      permission: permission?.name,
    }),
    continueButtonVariant: ButtonVariant.danger,
    continueButtonLabel: "clients:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPermission({
          id,
          type: permissionType,
          permissionId: permissionId,
        });
        addAlert(t("permissionDeletedSuccess"), AlertVariant.success);
        navigate(
          toAuthorizationTab({ realm, clientId: id, tab: "permissions" })
        );
      } catch (error) {
        addError("clients:permissionDeletedError", error);
      }
    },
  });

  const resourcesIds = useWatch({
    control,
    name: "resources",
    defaultValue: [],
  });

  if (!permission) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          permissionId
            ? permission.name!
            : `clients:create${toUpperCase(permissionType)}BasedPermission`
        }
        dropdownItems={
          permissionId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-resource"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="view-clients"
          onSubmit={handleSubmit(save)}
        >
          <FormProvider {...form}>
            <FormGroup
              label={t("common:name")}
              isRequired
              helperTextInvalid={t("common:required")}
              validated={errors.name ? "error" : "default"}
              fieldId="name"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:permissionName")}
                  fieldLabelId="name"
                />
              }
            >
              <KeycloakTextInput
                id="name"
                validated={errors.name ? "error" : "default"}
                {...register("name", { required: true })}
              />
            </FormGroup>
            <FormGroup
              label={t("common:description")}
              fieldId="description"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:permissionDescription")}
                  fieldLabelId="description"
                />
              }
              validated={errors.description ? "error" : "default"}
              helperTextInvalid={errors.description?.message}
            >
              <KeycloakTextArea
                id="description"
                validated={errors.description ? "error" : "default"}
                {...register("description", {
                  maxLength: {
                    value: 255,
                    message: t("common:maxLength", { length: 255 }),
                  },
                })}
              />
            </FormGroup>
            <FormGroup
              label={t("applyToResourceTypeFlag")}
              fieldId="applyToResourceTypeFlag"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:applyToResourceTypeFlag")}
                  fieldLabelId="clients:applyToResourceTypeFlag"
                />
              }
            >
              <Switch
                id="applyToResourceTypeFlag"
                name="applyToResourceTypeFlag"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={applyToResourceTypeFlag}
                onChange={setApplyToResourceTypeFlag}
                aria-label={t("applyToResourceTypeFlag")}
              />
            </FormGroup>
            {applyToResourceTypeFlag ? (
              <FormGroup
                label={t("resourceType")}
                fieldId="name"
                labelIcon={
                  <HelpItem
                    helpText={t("clients-help:resourceType")}
                    fieldLabelId="resourceType"
                  />
                }
                isRequired={permissionType === "scope"}
              >
                <KeycloakTextInput
                  id="resourceType"
                  {...register("resourceType", {
                    required: permissionType === "scope",
                  })}
                />
              </FormGroup>
            ) : (
              <FormGroup
                label={t("resources")}
                fieldId="resources"
                labelIcon={
                  <HelpItem
                    helpText={t("clients-help:permissionResources")}
                    fieldLabelId="clients:resources"
                  />
                }
                helperTextInvalid={t("common:required")}
                validated={errors.resources ? "error" : "default"}
                isRequired={permissionType !== "scope"}
              >
                <ResourcesPolicySelect
                  name="resources"
                  clientId={id}
                  permissionId={permissionId}
                  preSelected={
                    permissionType === "scope" ? undefined : selectedId
                  }
                  variant={
                    permissionType === "scope"
                      ? SelectVariant.typeahead
                      : SelectVariant.typeaheadMulti
                  }
                  isRequired={permissionType !== "scope"}
                />
              </FormGroup>
            )}
            {permissionType === "scope" && (
              <FormGroup
                label={t("authorizationScopes")}
                fieldId="scopes"
                labelIcon={
                  <HelpItem
                    helpText={t("clients-help:permissionScopes")}
                    fieldLabelId="clients:scopesSelect"
                  />
                }
                helperTextInvalid={t("common:required")}
                validated={errors.scopes ? "error" : "default"}
                isRequired
              >
                <ScopeSelect
                  clientId={id}
                  resourceId={resourcesIds?.[0]}
                  preSelected={selectedId}
                />
              </FormGroup>
            )}
            <FormGroup
              label={t("policies")}
              fieldId="policies"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:permissionPolicies")}
                  fieldLabelId="clients:policies"
                />
              }
            >
              <ResourcesPolicySelect
                name="policies"
                clientId={id}
                permissionId={permissionId}
              />
            </FormGroup>
            <FormGroup
              label={t("decisionStrategy")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:permissionDecisionStrategy")}
                  fieldLabelId="clients:decisionStrategy"
                />
              }
              fieldId="policyEnforcementMode"
              hasNoPaddingTop
            >
              <Controller
                name="decisionStrategy"
                data-testid="decisionStrategy"
                defaultValue={DecisionStrategy.UNANIMOUS}
                control={control}
                render={({ field }) => (
                  <>
                    {Object.values(DecisionStrategy).map((strategy) => (
                      <Radio
                        id={strategy}
                        key={strategy}
                        data-testid={strategy}
                        isChecked={field.value === strategy}
                        name="decisionStrategies"
                        onChange={() => field.onChange(strategy)}
                        label={t(`decisionStrategies.${strategy}`)}
                        className="pf-u-mb-md"
                      />
                    ))}
                  </>
                )}
              />
            </FormGroup>
            <ActionGroup>
              <div className="pf-u-mt-md">
                <Button
                  variant={ButtonVariant.primary}
                  type="submit"
                  data-testid="save"
                >
                  {t("common:save")}
                </Button>

                <Button
                  variant="link"
                  data-testid="cancel"
                  component={(props) => (
                    <Link
                      {...props}
                      to={toAuthorizationTab({
                        realm,
                        clientId: id,
                        tab: "permissions",
                      })}
                    ></Link>
                  )}
                >
                  {t("common:cancel")}
                </Button>
              </div>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
