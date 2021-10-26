import React, { useState } from "react";
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
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form-access/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAlerts } from "../components/alert/Alerts";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { Link, useHistory, useParams } from "react-router-dom";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import type { ClientProfileParams } from "./routes/ClientProfile";
import {
  COMPONENTS,
  isValidComponentType,
} from "../client-scopes/add/components/components";
import type ClientPolicyExecutorRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyExecutorRepresentation";

type ExecutorForm = Required<ClientPolicyExecutorRepresentation>;

const defaultValues: ExecutorForm = {
  configuration: {},
  executor: "",
};

export const ExecutorForm = () => {
  const { t } = useTranslation("realm-settings");
  const history = useHistory();
  const { realm, profileName } = useParams<ClientProfileParams>();
  const { addAlert, addError } = useAlerts();
  const [selectExecutorTypeOpen, setSelectExecutorTypeOpen] = useState(false);
  const serverInfo = useServerInfo();
  const adminClient = useAdminClient();
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
  const form = useForm<ExecutorForm>({ defaultValues });
  const { control } = form;

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({ includeGlobalProfiles: true }),
    (profiles) => {
      setGlobalProfiles(profiles.globalProfiles ?? []);
      setProfiles(profiles.profiles ?? []);
    },
    []
  );

  const fldNameFormatter = (name: string) =>
    name.toLowerCase().trim().split(/\s+/).join("-");

  const save = async () => {
    const formValues = form.getValues();
    const updatedProfiles = profiles.map((profile) => {
      if (profile.name !== profileName) {
        return profile;
      }

      const executors = (profile.executors ?? []).concat({
        executor: formValues.executor,
        configuration: formValues.configuration,
      });

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
      addAlert(t("realm-settings:addExecutorSuccess"), AlertVariant.success);
      history.push(`/${realm}/realm-settings/clientPolicies/${profileName}`);
    } catch (error) {
      addError("realm-settings:addExecutorError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey={t("addExecutor")} divider />
      <PageSection variant="light">
        <FormAccess isHorizontal role="manage-realm" className="pf-u-mt-lg">
          <FormGroup
            label={t("executorType")}
            fieldId="kc-executorType"
            labelIcon={
              executors.length > 0 && executors[0].helpText! !== "" ? (
                <HelpItem
                  helpText={executors[0].helpText}
                  forLabel={t("executorTypeHelpText")}
                  forID={t(`common:helpLabel`, {
                    label: t("executorTypeHelpText"),
                  })}
                />
              ) : undefined
            }
          >
            <Controller
              name="executor"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-executor"
                  placeholderText="Select an executor"
                  onToggle={(isOpen) => setSelectExecutorTypeOpen(isOpen)}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    const selectedExecutor = executorTypes?.filter(
                      (type) => type.id === value
                    );
                    setExecutors(selectedExecutor ?? []);
                    setExecutorProperties(
                      selectedExecutor?.[0].properties ?? []
                    );
                    setSelectExecutorTypeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  data-testid="executorType-select"
                  aria-label={t("executorType")}
                  isOpen={selectExecutorTypeOpen}
                  maxHeight={580}
                >
                  {executorTypes?.map((option) => (
                    <SelectOption
                      selected={option.id === value}
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
            {executorProperties.map((option) => {
              const componentType = option.type!;
              if (isValidComponentType(componentType)) {
                const Component = COMPONENTS[componentType];
                return (
                  <Component
                    key={option.name}
                    {...option}
                    name={fldNameFormatter(option.label!)}
                    label={option.label}
                  />
                );
              } else {
                console.warn(
                  `There is no editor registered for ${componentType}`
                );
              }
            })}
          </FormProvider>
          <ActionGroup>
            <Button
              variant="primary"
              onClick={save}
              data-testid="realm-settings-add-executor-save-button"
            >
              {t("common:add")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={`/${realm}/realm-settings/clientPolicies/${profileName}`}
                />
              )}
              data-testid="realm-settings-add-executor-cancel-button"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
