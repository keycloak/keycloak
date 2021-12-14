import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm } from "react-hook-form";
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

import { FormAccess } from "../components/form-access/FormAccess";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import type ClientPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyRepresentation";
import { camelCase } from "lodash";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useHistory, useParams } from "react-router";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { useRealm } from "../context/realm-context/RealmContext";
import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type ClientPolicyConditionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyConditionRepresentation";
import {
  EditClientPolicyParams,
  toEditClientPolicy,
} from "./routes/EditClientPolicy";
import type { EditClientPolicyConditionParams } from "./routes/EditCondition";
import {
  convertToMultiline,
  toValue,
} from "../components/multi-line-input/multi-line-convert";
import {
  COMPONENTS,
  isValidComponentType,
} from "../components/dynamic/components";
import { MultivaluedChipsComponent } from "../components/dynamic/MultivaluedChipsComponent";
import { MultivaluedRoleComponent } from "../components/dynamic/MultivaluedRoleComponent";
export type ItemType = { value: string };

type ConfigProperty = ConfigPropertyRepresentation & {
  config: any;
};

export default function NewClientPolicyCondition() {
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  const [openConditionType, setOpenConditionType] = useState(false);
  const [policies, setPolicies] = useState<ClientPolicyRepresentation[]>([]);

  const [condition, setCondition] = useState<
    ClientPolicyConditionRepresentation[]
  >([]);
  const [conditionData, setConditionData] =
    useState<ClientPolicyConditionRepresentation>();
  const [conditionType, setConditionType] = useState("");
  const [conditionProperties, setConditionProperties] = useState<
    ConfigPropertyRepresentation[]
  >([]);

  const { policyName } = useParams<EditClientPolicyParams>();
  const { conditionName } = useParams<EditClientPolicyConditionParams>();

  const serverInfo = useServerInfo();
  const form = useForm<ClientPolicyConditionRepresentation>({
    shouldUnregister: false,
  });

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const adminClient = useAdminClient();

  const setupForm = (
    condition: ClientPolicyConditionRepresentation,
    properties: ConfigPropertyRepresentation[]
  ) => {
    form.reset();

    Object.entries(condition.configuration!).map(([key, value]) => {
      const formKey = `config.${key}`;

      const property = properties.find((p) => p.name === key);
      if (
        property?.type === "MultivaluedString" &&
        property.name !== "scopes" &&
        property.name !== "groups"
      ) {
        form.setValue(formKey, convertToMultiline(value));
      } else if (property?.name === "client-scopes") {
        form.setValue("config.scopes", value);
      } else {
        form.setValue(formKey, value);
      }
    });
  };

  useFetch(
    () => adminClient.clientPolicies.listPolicies(),

    (policies) => {
      setPolicies(policies.policies ?? []);

      if (conditionName) {
        const currentPolicy = policies.policies?.find(
          (item) => item.name === policyName
        );

        const typeAndConfigData = currentPolicy?.conditions?.find(
          (item) => item.condition === conditionName
        );

        const currentCondition = conditionTypes?.find(
          (condition) => condition.id === conditionName
        );

        setConditionData(typeAndConfigData!);
        setConditionProperties(currentCondition?.properties!);
        setupForm(typeAndConfigData!, currentCondition?.properties!);
      }
    },
    []
  );

  const save = async (configPolicy: ConfigProperty) => {
    const configValues = configPolicy.config;

    const writeConfig = () => {
      return conditionProperties.reduce((r: any, p) => {
        p.type === "MultivaluedString" &&
        p.name !== "scopes" &&
        p.name !== "groups"
          ? (r[p.name!] = toValue(configValues[p.name!]))
          : (r[p.name!] = configValues[p.name!]);
        return r;
      }, {});
    };

    const updatedPolicies = policies.map((policy) => {
      if (policy.name !== policyName) {
        return policy;
      }

      let conditions = policy.conditions ?? [];

      if (conditionName) {
        const createdCondition = {
          condition: conditionData?.condition,
          configuration: writeConfig(),
        };

        const index = conditions.findIndex(
          (condition) => conditionName === condition.condition
        );

        if (index === -1) {
          return;
        }

        const newConditions = [
          ...conditions.slice(0, index),
          createdCondition,
          ...conditions.slice(index + 1),
        ];

        return {
          ...policy,
          conditions: newConditions,
        };
      }

      conditions = conditions.concat({
        condition: condition[0].condition,
        configuration: writeConfig(),
      });

      return {
        ...policy,
        conditions,
      };
    }) as ClientPolicyRepresentation[];

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: updatedPolicies,
      });
      setPolicies(updatedPolicies);
      history.push(
        `/${realm}/realm-settings/clientPolicies/${policyName}/edit-policy`
      );
      addAlert(
        conditionName
          ? t("realm-settings:updateClientConditionSuccess")
          : t("realm-settings:createClientConditionSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:createClientConditionError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormPanel
        className="kc-login-screen"
        title={conditionName ? t("editCondition") : t("addCondition")}
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("conditionType")}
            fieldId="conditionType"
            labelIcon={
              <HelpItem
                helpText={
                  conditionType
                    ? `realm-settings-help:${camelCase(
                        conditionType.replace(/-/g, " ")
                      )}`
                    : "realm-settings:anyClient"
                }
                fieldLabelId="realm-settings:conditionType"
              />
            }
          >
            <Controller
              name="conditions"
              defaultValue={"any-client"}
              control={form.control}
              render={({ onChange, value }) => (
                <Select
                  placeholderText={t("selectACondition")}
                  className="kc-conditionType-select"
                  data-testid="conditionType-select"
                  toggleId="provider"
                  isDisabled={!!conditionName}
                  onToggle={(toggle) => setOpenConditionType(toggle)}
                  onSelect={(_, value) => {
                    onChange(value);
                    setConditionProperties(
                      (value as ComponentTypeRepresentation).properties
                    );
                    setConditionType((value as ComponentTypeRepresentation).id);
                    setCondition([
                      {
                        condition: (value as ComponentTypeRepresentation).id,
                      },
                    ]);
                    setOpenConditionType(false);
                  }}
                  selections={conditionName ? conditionName : conditionType}
                  variant={SelectVariant.single}
                  aria-label={t("conditionType")}
                  isOpen={openConditionType}
                >
                  {conditionTypes?.map((condition) => (
                    <SelectOption
                      selected={condition.id === value}
                      description={t(
                        `realm-settings-help:${camelCase(
                          condition.id.replace(/-/g, " ")
                        )}`
                      )}
                      key={condition.id}
                      value={condition}
                    >
                      {condition.id}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>

          <FormProvider {...form}>
            {conditionProperties.map((property) => {
              const componentType = property.type!;
              if (property.name === "roles") {
                return <MultivaluedRoleComponent {...property} />;
              }

              if (property.name === "scopes" || property.name === "groups") {
                return (
                  <MultivaluedChipsComponent
                    defaultValue={
                      property.name === "scopes" ? "offline_access" : "topgroup"
                    }
                    {...property}
                  />
                );
              }

              if (isValidComponentType(componentType)) {
                const Component = COMPONENTS[componentType];
                return <Component key={property.name} {...property} />;
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
              type="submit"
              data-testid="addCondition-saveBtn"
              isDisabled={conditionType === "" && !conditionName}
            >
              {conditionName ? t("common:save") : t("common:add")}
            </Button>
            <Button
              variant="link"
              data-testid="addCondition-cancelBtn"
              onClick={() =>
                history.push(toEditClientPolicy({ realm, policyName }))
              }
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
}
