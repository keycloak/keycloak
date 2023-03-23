import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { useAlerts } from "../components/alert/Alerts";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useParams } from "../utils/useParams";
import { ClientProfileParams, toClientProfile } from "./routes/ClientProfile";
import type { ExecutorParams } from "./routes/Executor";

type ExecutorForm = {
  config?: object;
  executor: string;
};

const defaultValues: ExecutorForm = {
  config: {},
  executor: "",
};

export default function ExecutorForm() {
  const { t } = useTranslation("realm-settings");
  const navigate = useNavigate();
  const { realm, profileName } = useParams<ClientProfileParams>();
  const { executorName } = useParams<ExecutorParams>();
  const { addAlert, addError } = useAlerts();
  const [selectExecutorTypeOpen, setSelectExecutorTypeOpen] = useState(false);
  const serverInfo = useServerInfo();
  const { adminClient } = useAdminClient();
  const executorTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider"
    ];
  const [executors, setExecutors] = useState<ComponentTypeRepresentation[]>([]);
  const [executorProperties, setExecutorProperties] = useState<
    ConfigPropertyRepresentation[]
  >([]);
  const [globalProfiles, setGlobalProfiles] = useState<
    ClientProfileRepresentation[]
  >([]);
  const [profiles, setProfiles] = useState<ClientProfileRepresentation[]>([]);
  const form = useForm({ defaultValues });
  const { control, reset, handleSubmit } = form;
  const editMode = !!executorName;

  const setupForm = (profiles: ClientProfileRepresentation[]) => {
    const profile = profiles.find((profile) => profile.name === profileName);
    const executor = profile?.executors?.find(
      (executor) => executor.executor === executorName
    );
    if (executor) reset({ config: executor.configuration });
  };

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({ includeGlobalProfiles: true }),
    (profiles) => {
      setGlobalProfiles(profiles.globalProfiles!);
      setProfiles(profiles.profiles!);

      setupForm(profiles.profiles!);
      setupForm(profiles.globalProfiles!);
    },
    []
  );

  const save = async () => {
    const formValues = form.getValues();
    const updatedProfiles = profiles.map((profile) => {
      if (profile.name !== profileName) {
        return profile;
      }

      const executors = (profile.executors ?? []).concat({
        executor: formValues.executor,
        configuration: formValues.config || {},
      });

      if (editMode) {
        const profileExecutor = profile.executors!.find(
          (executor) => executor.executor === executorName
        );
        profileExecutor!.configuration = {
          ...profileExecutor!.configuration,
          ...formValues.config,
        };
      }

      if (editMode) {
        return profile;
      }
      return {
        ...profile,
        executors,
      };
    });
    try {
      await adminClient.clientPolicies.createProfiles({
        profiles: updatedProfiles,
        globalProfiles: globalProfiles,
      });
      addAlert(
        editMode
          ? t("realm-settings:updateExecutorSuccess")
          : t("realm-settings:addExecutorSuccess"),
        AlertVariant.success
      );

      navigate(toClientProfile({ realm, profileName }));
    } catch (error) {
      addError(
        editMode
          ? "realm-settings:updateExecutorError"
          : "realm-settings:addExecutorError",
        error
      );
    }
  };

  const globalProfile = globalProfiles.find(
    (globalProfile) => globalProfile.name === profileName
  );

  const profileExecutorType = executorTypes?.find(
    (executor) => executor.id === executorName
  );

  const editedProfileExecutors =
    profileExecutorType?.properties.map<ConfigPropertyRepresentation>(
      (property) => {
        const globalDefaultValues = editMode ? property.defaultValue : "";
        return {
          ...property,
          defaultValue: globalDefaultValues,
        };
      }
    );

  return (
    <>
      <ViewHeader
        titleKey={editMode ? executorName : t("addExecutor")}
        divider
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          isReadOnly={!!globalProfile}
        >
          <FormGroup
            label={t("executorType")}
            fieldId="kc-executorType"
            labelIcon={
              executors.length > 0 && executors[0].helpText! !== "" ? (
                <HelpItem
                  helpText={executors[0].helpText}
                  fieldLabelId="realm-settings:executorTypeHelpText"
                />
              ) : editMode ? (
                <HelpItem
                  helpText={profileExecutorType?.helpText}
                  fieldLabelId="realm-settings:executorTypeHelpText"
                />
              ) : undefined
            }
          >
            <Controller
              name="executor"
              defaultValue=""
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="kc-executor"
                  placeholderText="Select an executor"
                  onToggle={(isOpen) => setSelectExecutorTypeOpen(isOpen)}
                  onSelect={(_, value) => {
                    reset({ ...defaultValues, executor: value.toString() });
                    const selectedExecutor = executorTypes?.filter(
                      (type) => type.id === value
                    );
                    setExecutors(selectedExecutor ?? []);
                    setExecutorProperties(
                      selectedExecutor?.[0].properties ?? []
                    );
                    setSelectExecutorTypeOpen(false);
                  }}
                  selections={editMode ? executorName : field.value}
                  variant={SelectVariant.single}
                  data-testid="executorType-select"
                  aria-label={t("executorType")}
                  isOpen={selectExecutorTypeOpen}
                  maxHeight={580}
                  isDisabled={editMode}
                >
                  {executorTypes?.map((option) => (
                    <SelectOption
                      selected={option.id === field.value}
                      key={option.id}
                      value={option.id}
                      description={option.helpText}
                    />
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormProvider {...form}>
            <DynamicComponents
              properties={
                editMode ? editedProfileExecutors! : executorProperties
              }
            />
          </FormProvider>
          {!globalProfile && (
            <ActionGroup>
              <Button
                variant="primary"
                onClick={() => handleSubmit(save)()}
                data-testid="addExecutor-saveBtn"
              >
                {editMode ? t("common:save") : t("common:add")}
              </Button>
              <Button
                variant="link"
                component={(props) => (
                  <Link
                    {...props}
                    to={toClientProfile({ realm, profileName })}
                  />
                )}
                data-testid="addExecutor-cancelBtn"
              >
                {t("common:cancel")}
              </Button>
            </ActionGroup>
          )}
        </FormAccess>
        {editMode && globalProfile && (
          <div className="kc-backToProfile">
            <Button
              component={(props) => (
                <Link {...props} to={toClientProfile({ realm, profileName })} />
              )}
              variant="primary"
            >
              {t("realm-settings:back")}
            </Button>
          </div>
        )}
      </PageSection>
    </>
  );
}
