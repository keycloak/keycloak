import {
  AlertVariant,
  Button,
  Divider,
  FormGroup,
  PageSection,
  Radio,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useAlerts } from "../../components/alert/Alerts";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import useToggle from "../../utils/useToggle";
import { DecisionStrategySelect } from "./DecisionStrategySelect";
import { ImportDialog } from "./ImportDialog";
import { useFetch } from "../../utils/useFetch";
import { useAccess } from "../../context/access/Access";

const POLICY_ENFORCEMENT_MODES = [
  "ENFORCING",
  "PERMISSIVE",
  "DISABLED",
] as const;

export type FormFields = Omit<
  ResourceServerRepresentation,
  "scopes" | "resources"
>;

export const AuthorizationSettings = ({ clientId }: { clientId: string }) => {
  const { t } = useTranslation();
  const [resource, setResource] = useState<ResourceServerRepresentation>();
  const [importDialog, toggleImportDialog] = useToggle();

  const form = useForm<FormFields>({});
  const { control, reset, handleSubmit } = form;

  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();

  const isDisabled = !hasAccess("manage-authorization");

  useFetch(
    () => adminClient.clients.getResourceServer({ id: clientId }),
    (resource) => {
      setResource(resource);
      reset(resource);
    },
    [],
  );

  const importResource = async (value: ResourceServerRepresentation) => {
    try {
      await adminClient.clients.importResource({ id: clientId }, value);
      addAlert(t("importResourceSuccess"), AlertVariant.success);
      reset({ ...value });
    } catch (error) {
      addError("importResourceError", error);
    }
  };

  const onSubmit = async (resource: ResourceServerRepresentation) => {
    try {
      await adminClient.clients.updateResourceServer(
        { id: clientId },
        resource,
      );
      addAlert(t("updateResourceSuccess"), AlertVariant.success);
    } catch (error) {
      addError("resourceSaveError", error);
    }
  };

  if (!resource) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      {importDialog && (
        <ImportDialog
          onConfirm={importResource}
          closeDialog={toggleImportDialog}
        />
      )}
      <FormAccess
        role="manage-authorization"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
      >
        <FormGroup
          label={t("import")}
          fieldId="import"
          labelIcon={
            <HelpItem helpText={t("importHelp")} fieldLabelId="import" />
          }
        >
          <Button variant="secondary" onClick={toggleImportDialog}>
            {t("import")}
          </Button>
        </FormGroup>
        <Divider />
        <FormGroup
          label={t("policyEnforcementMode")}
          labelIcon={
            <HelpItem
              helpText={t("policyEnforcementModeHelp")}
              fieldLabelId="policyEnforcementMode"
            />
          }
          fieldId="policyEnforcementMode"
          hasNoPaddingTop
        >
          <Controller
            name="policyEnforcementMode"
            data-testid="policyEnforcementMode"
            defaultValue={POLICY_ENFORCEMENT_MODES[0]}
            control={control}
            render={({ field }) => (
              <>
                {POLICY_ENFORCEMENT_MODES.map((mode) => (
                  <Radio
                    id={mode}
                    key={mode}
                    data-testid={mode}
                    isChecked={field.value === mode}
                    isDisabled={isDisabled}
                    name="policyEnforcementMode"
                    onChange={() => field.onChange(mode)}
                    label={t(`policyEnforcementModes.${mode}`)}
                    className="pf-u-mb-md"
                  />
                ))}
              </>
            )}
          />
        </FormGroup>
        <FormProvider {...form}>
          <DecisionStrategySelect isLimited />
        </FormProvider>
        <FormGroup
          hasNoPaddingTop
          label={t("allowRemoteResourceManagement")}
          fieldId="allowRemoteResourceManagement"
          labelIcon={
            <HelpItem
              helpText={t("allowRemoteResourceManagementHelp")}
              fieldLabelId="allowRemoteResourceManagement"
            />
          }
        >
          <Controller
            name="allowRemoteResourceManagement"
            data-testid="allowRemoteResourceManagement"
            defaultValue={false}
            control={control}
            render={({ field }) => (
              <Switch
                id="allowRemoteResourceManagement"
                label={t("on")}
                labelOff={t("off")}
                isChecked={field.value}
                onChange={field.onChange}
                aria-label={t("allowRemoteResourceManagement")}
              />
            )}
          />
        </FormGroup>
        <FixedButtonsGroup
          name="authenticationSettings"
          reset={() => reset(resource)}
          isActive
          isSubmit
        />
      </FormAccess>
    </PageSection>
  );
};
