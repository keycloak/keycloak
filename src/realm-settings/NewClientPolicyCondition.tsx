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
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import {
  EditClientPolicyParams,
  toEditClientPolicy,
} from "./routes/EditClientPolicy";
import type { EditClientPolicyConditionParams } from "./routes/EditCondition";
import { convertToMultiline } from "../components/multi-line-input/MultiLineInput";

export type ItemType = { value: string };

export default function NewClientPolicyCondition() {
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  const { handleSubmit, control } = useForm<ClientPolicyRepresentation>({
    mode: "onChange",
  });

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

  const [selectedVals, setSelectedVals] = useState<any>();

  const { policyName } = useParams<EditClientPolicyParams>();
  const { conditionName } = useParams<EditClientPolicyConditionParams>();

  const serverInfo = useServerInfo();
  const form = useForm();

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const adminClient = useAdminClient();

  const setupForm = (condition: ClientPolicyConditionRepresentation) => {
    form.reset();

    Object.entries(condition).map(([key, value]) => {
      if (key === "configuration") {
        if (
          conditionName === "client-roles" ||
          conditionName === "client-updater-source-roles"
        ) {
          form.setValue("config.roles", convertToMultiline(value["roles"]));
        } else if (conditionName === "client-scopes") {
          form.setValue("config.scopes", convertToMultiline(value["scopes"]));
          form.setValue("config.type", value["type"]);
        } else if (conditionName === "client-updater-source-groups") {
          form.setValue("config.groups", convertToMultiline(value["groups"]));
        } else if (conditionName === "client-updater-source-host") {
          form.setValue(
            "config.trusted-hosts",
            convertToMultiline(value["trusted-hosts"])
          );
        } else if (conditionName === "client-updater-context") {
          form.setValue(
            "config.update-client-source",
            value["update-client-source"][0]["update-client-source"]
          );
        } else if (conditionName === "client-access-type") {
          form.setValue("config.type", value.type[0]);
        }
      }
      form.setValue(key, value);
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
        setSelectedVals(Object.values(typeAndConfigData?.configuration!)[0][0]);
        setConditionProperties(currentCondition?.properties!);
        setupForm(typeAndConfigData!);
      }
    },
    []
  );

  const save = async () => {
    const formValues = form.getValues();
    const configValues = formValues.config;

    const writeConfig = () => {
      if (
        condition[0]?.condition === "any-client" ||
        conditionName === "any-client"
      ) {
        return {};
      } else if (
        condition[0]?.condition === "client-access-type" ||
        conditionName === "client-access-type"
      ) {
        return { type: [formValues.config.type] };
      } else if (
        condition[0]?.condition === "client-updater-context" ||
        conditionName === "client-updater-context"
      ) {
        return {
          "update-client-source": [Object.values(formValues)[0]],
        };
      } else if (
        condition[0]?.condition === "client-scopes" ||
        conditionName === "client-scopes"
      ) {
        return {
          type: Object.values(formValues)[0].type,
          scopes: (Object.values(formValues)[0].scopes as ItemType[]).map(
            (item) => (item as ItemType).value
          ),
        };
      } else
        return {
          [Object.keys(configValues)[0]]: Object.values(
            configValues?.[Object.keys(configValues)[0]]
          ).map((item) => (item as ItemType).value),
        };
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

  const handleCallback = (childData: any) => {
    setSelectedVals(childData);
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
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("conditionType")}
            fieldId="conditionType"
            labelIcon={
              <HelpItem
                helpText={
                  conditionType
                    ? t(
                        `realm-settings-help:${camelCase(
                          conditionType.replace(/-/g, " ")
                        )}`
                      )
                    : t("anyClient")
                }
                forLabel={t("conditionType")}
                forID="conditionType"
              />
            }
          >
            <Controller
              name="conditions"
              defaultValue={"any-client"}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  placeholderText={t("selectACondition")}
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
            <DynamicComponents
              properties={conditionProperties}
              selectedValues={
                conditionName === "client-access-type"
                  ? selectedVals
                  : conditionName === "client-updater-context"
                  ? selectedVals?.["update-client-source"]
                  : []
              }
              parentCallback={handleCallback}
            />
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
