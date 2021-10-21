import React, { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
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
import type ClientPolicyConditionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyConditionRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { useRealm } from "../context/realm-context/RealmContext";
import type { EditClientPolicyParams } from "./routes/EditClientPolicy";

export const NewClientPolicyCondition = () => {
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

  const { policyName } = useParams<EditClientPolicyParams>();

  const serverInfo = useServerInfo();

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const adminClient = useAdminClient();

  const currentPolicy = useMemo(
    () => policies.find(({ name }) => name === policyName),
    [policies, policyName]
  );

  useFetch(
    () => adminClient.clientPolicies.listPolicies(),
    (policies) => {
      setPolicies(policies.policies ?? []);
    },
    []
  );

  const save = async () => {
    const createdPolicy = {
      ...currentPolicy,
      profiles: [],
      conditions: currentPolicy?.conditions?.concat(condition),
    };

    const index = policies.findIndex(
      (policy) => createdPolicy.name === policy.name
    );

    if (index === -1) {
      return;
    }

    const newPolicies = [
      ...policies.slice(0, index),
      createdPolicy,
      ...policies.slice(index + 1),
    ];

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: newPolicies,
      });
      setPolicies(newPolicies);
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
                    ? t(`${camelCase(conditionType.replace(/-/g, " "))}`)
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
                    setConditionType((value as ComponentTypeRepresentation).id);
                    setCondition([
                      {
                        condition: (value as ComponentTypeRepresentation).id,
                        configuration: {},
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
                        `${camelCase(condition.id.replace(/-/g, " "))}`
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
};
