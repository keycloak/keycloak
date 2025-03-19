import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type ClientPolicyConditionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyConditionRepresentation";
import type ClientPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  SelectOption,
} from "@patternfly/react-core";
import { camelCase } from "lodash-es";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { FormAccess } from "../components/form/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { toEditClientPolicy } from "./routes/EditClientPolicy";
import type { EditClientPolicyConditionParams } from "./routes/EditCondition";

export type ItemType = { value: string };

type ConfigProperty = ConfigPropertyRepresentation & {
  conditions: any;
  config: any;
};

export default function NewClientPolicyCondition() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const [openConditionType, setOpenConditionType] = useState(false);
  const [isGlobalPolicy, setIsGlobalPolicy] = useState(false);
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

  const { policyName, conditionName } =
    useParams<EditClientPolicyConditionParams>();

  const serverInfo = useServerInfo();
  const form = useForm<ConfigProperty>();

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const setupForm = (condition: ClientPolicyConditionRepresentation) => {
    form.reset({ config: condition.configuration || {} });
  };

  useFetch(
    () =>
      adminClient.clientPolicies.listPolicies({
        includeGlobalPolicies: true,
      }),

    (policies) => {
      setPolicies(policies.policies ?? []);

      if (conditionName) {
        let currentPolicy = policies.policies?.find(
          (item) => item.name === policyName,
        );
        if (currentPolicy === undefined) {
          currentPolicy = policies.globalPolicies?.find(
            (item) => item.name === policyName,
          );
          setIsGlobalPolicy(currentPolicy !== undefined);
        }

        const typeAndConfigData = currentPolicy?.conditions?.find(
          (item) => item.condition === conditionName,
        );

        const currentCondition = conditionTypes?.find(
          (condition) => condition.id === conditionName,
        );

        setConditionData(typeAndConfigData!);
        setConditionProperties(currentCondition?.properties!);
        setupForm(typeAndConfigData!);
      }
    },
    [],
  );

  const save = async (configPolicy: ConfigProperty) => {
    const configValues = configPolicy.config;

    const writeConfig = () => {
      return conditionProperties.reduce((r: any, p) => {
        r[p.name!] = configValues[p.name!];
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
          (condition) => conditionName === condition.condition,
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
      navigate(toEditClientPolicy({ realm, policyName: policyName! }));
      addAlert(
        conditionName
          ? t("updateClientConditionSuccess")
          : t("createClientConditionSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("createClientConditionError", error);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={
          conditionName
            ? isGlobalPolicy
              ? t("viewCondition")
              : t("editCondition")
            : t("addCondition")
        }
        divider
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="manage-realm"
          isReadOnly={isGlobalPolicy}
          className="pf-v5-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("conditionType")}
            fieldId="conditionType"
            labelIcon={
              <HelpItem
                helpText={
                  conditionType
                    ? `${camelCase(conditionType.replace(/-/g, " "))}Help`
                    : "conditionsHelp"
                }
                fieldLabelId="conditionType"
              />
            }
          >
            <Controller
              name="conditions"
              defaultValue={"any-client"}
              control={form.control}
              render={({ field }) => (
                <KeycloakSelect
                  placeholderText={t("selectACondition")}
                  className="kc-conditionType-select"
                  data-testid="conditionType-select"
                  toggleId="provider"
                  isDisabled={!!conditionName}
                  onToggle={(toggle) => setOpenConditionType(toggle)}
                  onSelect={(value) => {
                    field.onChange(value);
                    setConditionProperties(
                      (value as ComponentTypeRepresentation).properties,
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
                      data-testid={condition.id}
                      selected={condition.id === field.value}
                      description={t(
                        camelCase(condition.id.replace(/-/g, " ")),
                      )}
                      key={condition.id}
                      value={condition}
                    >
                      {condition.id}
                    </SelectOption>
                  ))}
                </KeycloakSelect>
              )}
            />
          </FormGroup>

          <FormProvider {...form}>
            <DynamicComponents properties={conditionProperties} />
          </FormProvider>
          {!isGlobalPolicy && (
            <ActionGroup>
              <Button
                variant="primary"
                type="submit"
                data-testid="addCondition-saveBtn"
                isDisabled={
                  conditionType === "" && !conditionName && isGlobalPolicy
                }
              >
                {conditionName ? t("save") : t("add")}
              </Button>
              <Button
                variant="link"
                data-testid="addCondition-cancelBtn"
                onClick={() =>
                  navigate(
                    toEditClientPolicy({ realm, policyName: policyName! }),
                  )
                }
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          )}
        </FormAccess>
        {isGlobalPolicy && (
          <div className="kc-backToProfile">
            <Button
              component={(props) => (
                <Link
                  {...props}
                  to={toEditClientPolicy({ realm, policyName: policyName! })}
                />
              )}
              variant="primary"
            >
              {t("back")}
            </Button>
          </div>
        )}
      </PageSection>
    </>
  );
}
