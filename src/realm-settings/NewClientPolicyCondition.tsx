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
import type { EditClientPolicyParams } from "./routes/EditClientPolicy";
import {
  COMPONENTS,
  isValidComponentType,
} from "../client-scopes/add/components/components";
import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type ClientPolicyConditionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyConditionRepresentation";

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
  const [conditionType, setConditionType] = useState("");
  const [conditionProperties, setConditionProperties] = useState<
    ConfigPropertyRepresentation[]
  >([]);

  const { policyName } = useParams<EditClientPolicyParams>();

  const serverInfo = useServerInfo();
  const form = useForm();

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const adminClient = useAdminClient();

  useFetch(
    () => adminClient.clientPolicies.listPolicies(),
    (policies) => {
      setPolicies(policies.policies ?? []);
    },
    []
  );

  const save = async () => {
    const updatedPolicies = policies.map((policy) => {
      if (policy.name !== policyName) {
        return policy;
      }

      const formValues = form.getValues();
      const configValues = formValues.config;

      const writeConfig = () => {
        if (condition[0]?.condition === "any-client") {
          return {};
        } else if (condition[0]?.condition === "client-access-type") {
          return { type: [formValues["client-accesstype"].label] };
        } else if (condition[0]?.condition === "client-updater-context") {
          return {
            "update-client-source": [Object.values(formValues)[0]],
          };
        } else if (condition[0]?.condition === "client-scopes") {
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

      const conditions = (policy.conditions ?? []).concat({
        condition: condition[0].condition,
        configuration: writeConfig(),
      });

      return {
        ...policy,
        conditions,
      };
    });

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: updatedPolicies,
      });
      setPolicies(updatedPolicies);
      history.push(
        `/${realm}/realm-settings/clientPolicies/${policyName}/edit-policy`
      );
      addAlert(
        t("realm-settings:createClientConditionSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:createClientConditionError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormPanel className="kc-login-screen" title={t("addCondition")}>
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
                  toggleId="provider"
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
                  selections={conditionType}
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
            {conditionProperties.map((option) => {
              const componentType = option.type!;
              if (isValidComponentType(componentType)) {
                const Component = COMPONENTS[componentType];
                return (
                  <Component
                    key={option.name}
                    {...option}
                    name={option.name}
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
              type="submit"
              data-testid="edit-policy-tab-save"
              isDisabled={conditionType === ""}
            >
              {t("common:add")}
            </Button>
            <Button
              variant="link"
              onClick={() =>
                history.push(
                  `/${realm}/realm-settings/clientPolicies/${policyName}/edit-policy`
                )
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
