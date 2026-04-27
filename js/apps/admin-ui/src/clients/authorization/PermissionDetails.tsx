import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import { DecisionStrategy } from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import {
  FormErrorText,
  HelpItem,
  SelectVariant,
  TextAreaControl,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  Radio,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAccess } from "../../context/access/Access";
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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const form = useForm<FormFields>({
    mode: "onChange",
  });
  const {
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
        setPermission(result);
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
            <TextControl
              name="name"
              label={t("name")}
              labelIcon={t("permissionName")}
              rules={{
                required: t("required"),
              }}
            />
            <TextAreaControl
              name="description"
              label={t("description")}
              labelIcon={t("permissionDescription")}
              rules={{
                maxLength: {
                  value: 255,
                  message: t("maxLength", { length: 255 }),
                },
              }}
            />
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
                onChange={(_event, val) => setApplyToResourceTypeFlag(val)}
                aria-label={t("applyToResourceTypeFlag")}
              />
            </FormGroup>
            {applyToResourceTypeFlag ? (
              <TextControl
                name="resourceType"
                label={t("resourceType")}
                labelIcon={t("resourceTypeHelp")}
                rules={{
                  required: {
                    value: permissionType === "scope" ? true : false,
                    message: t("required"),
                  },
                }}
              />
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
                {errors.resources && <FormErrorText message={t("required")} />}
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
                isRequired
              >
                <ScopeSelect
                  clientId={id}
                  resourceId={resourcesIds?.[0]}
                  preSelected={selectedId}
                />
                {errors.scopes && <FormErrorText message={t("required")} />}
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
                        className="pf-v5-u-mb-md"
                      />
                    ))}
                  </>
                )}
              />
            </FormGroup>
            <ActionGroup>
              <div className="pf-v5-u-mt-md">
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
