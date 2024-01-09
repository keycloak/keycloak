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
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useFetch } from "../../utils/useFetch";
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
import { useAccess } from "../../context/access/Access";

type FormFields = PolicyRepresentation & {
  resourceType: string;
};

export default function PermissionDetails() {
  const { t } = useTranslation();

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

  const { addAlert, addError } = useAlerts();
  const [permission, setPermission] = useState<PolicyRepresentation>();
  const [applyToResourceTypeFlag, setApplyToResourceTypeFlag] = useState(false);
  const { hasAccess } = useAccess();

  const isDisabled = !hasAccess("manage-authorization");

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
        throw new Error(t("notFound"));
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
          !!(permission as { resourceType: string }).resourceType,
        );
      }
      setPermission({ ...permission, resources, policies });
    },
    [],
  );

  const save = async (permission: PolicyRepresentation) => {
    try {
      if (permissionId) {
        await adminClient.clients.updatePermission(
          { id, type: permissionType, permissionId },
          permission,
        );
      } else {
        const result = await adminClient.clients.createPermission(
          { id, type: permissionType },
          permission,
        );
        navigate(
          toPermissionDetails({
            realm,
            id,
            permissionType,
            permissionId: result.id!,
          }),
        );
      }
      addAlert(
        t((permissionId ? "update" : "create") + "PermissionSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("permissionSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deletePermission",
    messageKey: t("deletePermissionConfirm", {
      permission: permission?.name,
    }),
    continueButtonVariant: ButtonVariant.danger,
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPermission({
          id,
          type: permissionType,
          permissionId: permissionId,
        });
        addAlert(t("permissionDeletedSuccess"), AlertVariant.success);
        navigate(
          toAuthorizationTab({ realm, clientId: id, tab: "permissions" }),
        );
      } catch (error) {
        addError("permissionDeletedError", error);
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
            : `create${toUpperCase(permissionType)}BasedPermission`
        }
        dropdownItems={
          permissionId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-resource"
                  isDisabled={isDisabled}
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="manage-authorization"
          onSubmit={handleSubmit(save)}
        >
          <FormProvider {...form}>
            <FormGroup
              label={t("name")}
              isRequired
              helperTextInvalid={t("required")}
              validated={errors.name ? "error" : "default"}
              fieldId="name"
              labelIcon={
                <HelpItem helpText={t("permissionName")} fieldLabelId="name" />
              }
            >
              <KeycloakTextInput
                id="name"
                validated={errors.name ? "error" : "default"}
                {...register("name", { required: true })}
              />
            </FormGroup>
            <FormGroup
              label={t("description")}
              fieldId="description"
              labelIcon={
                <HelpItem
                  helpText={t("permissionDescription")}
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
                    message: t("maxLength", { length: 255 }),
                  },
                })}
              />
            </FormGroup>
            <FormGroup
              label={t("applyToResourceTypeFlag")}
              fieldId="applyToResourceTypeFlag"
              labelIcon={
                <HelpItem
                  helpText={t("applyToResourceTypeFlagHelp")}
                  fieldLabelId="applyToResourceTypeFlag"
                />
              }
            >
              <Switch
                id="applyToResourceTypeFlag"
                name="applyToResourceTypeFlag"
                label={t("on")}
                labelOff={t("off")}
                isChecked={applyToResourceTypeFlag}
                onChange={setApplyToResourceTypeFlag}
                aria-label={t("applyToResourceTypeFlag")}
              />
            </FormGroup>
            {applyToResourceTypeFlag ? (
              <FormGroup
                label={t("resourceType")}
                fieldId="resourceType"
                labelIcon={
                  <HelpItem
                    helpText={t("resourceTypeHelp")}
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
                label={t("resource")}
                fieldId="resources"
                labelIcon={
                  <HelpItem
                    helpText={t("permissionResources")}
                    fieldLabelId="resources"
                  />
                }
                helperTextInvalid={t("required")}
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
                    helpText={t("permissionScopesHelp")}
                    fieldLabelId="scopesSelect"
                  />
                }
                helperTextInvalid={t("required")}
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
                  helpText={t("permissionPoliciesHelp")}
                  fieldLabelId="policies"
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
                  helpText={t("permissionDecisionStrategyHelp")}
                  fieldLabelId="decisionStrategy"
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
                        isDisabled={isDisabled}
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
                  {t("save")}
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
                  {t("cancel")}
                </Button>
              </div>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
