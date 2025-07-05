import type ClientPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyRepresentation";
import {
  Action,
  KeycloakDataTable,
  KeycloakSpinner,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Flex,
  FlexItem,
  PageSection,
  Radio,
  Switch,
  Title,
  ToolbarItem,
} from "@patternfly/react-core";
import { omit } from "lodash-es";
import { useState } from "react";
import { Controller, useForm, type UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import CodeEditor from "../components/form/CodeEditor";
import { useRealm } from "../context/realm-context/RealmContext";
import { prettyPrintJSON } from "../util";
import { translationFormatter } from "../utils/translationFormatter";
import { toAddClientPolicy } from "./routes/AddClientPolicy";
import { toClientPolicies } from "./routes/ClientPolicies";
import { toEditClientPolicy } from "./routes/EditClientPolicy";

import "./realm-settings-section.css";

type ClientPolicy = ClientPolicyRepresentation & {
  global?: boolean;
};

export const PoliciesTab = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [show, setShow] = useState(false);
  const [policies, setPolicies] = useState<ClientPolicy[]>();
  const [selectedPolicy, setSelectedPolicy] = useState<ClientPolicy>();
  const [key, setKey] = useState(0);
  const [code, setCode] = useState<string>();
  const [tablePolicies, setTablePolicies] = useState<ClientPolicy[]>();
  const refresh = () => setKey(key + 1);

  const form = useForm<Record<string, boolean>>({ mode: "onChange" });

  useFetch(
    () =>
      adminClient.clientPolicies.listPolicies({
        includeGlobalPolicies: true,
      }),
    (allPolicies) => {
      const globalPolicies = allPolicies.globalPolicies?.map(
        (globalPolicies) => ({
          ...globalPolicies,
          global: true,
        }),
      );

      const policies = allPolicies.policies?.map((policies) => ({
        ...policies,
        global: false,
      }));

      const allClientPolicies = globalPolicies?.concat(policies ?? []);

      setPolicies(allClientPolicies);
      setTablePolicies(allClientPolicies || []);
      setCode(prettyPrintJSON(allClientPolicies));
    },
    [key],
  );

  const loader = async () => policies ?? [];

  const saveStatus = async () => {
    const switchValues = form.getValues();

    const updatedPolicies = policies
      ?.filter((policy) => {
        return !policy.global;
      })
      .map<ClientPolicyRepresentation>((policy) => {
        const enabled = switchValues[policy.name!];
        const enabledPolicy = {
          ...policy,
          enabled,
        };
        delete enabledPolicy.global;
        return enabledPolicy;
      });

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: updatedPolicies,
      });
      navigate(toClientPolicies({ realm, tab: "policies" }));
      addAlert(t("updateClientPolicySuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateClientPolicyError", error);
    }
  };

  const normalizePolicy = (policy: ClientPolicy): ClientPolicyRepresentation =>
    omit(policy, "global");

  const save = async () => {
    if (!code) {
      return;
    }

    try {
      const obj: ClientPolicy[] = JSON.parse(code);

      const changedPolicies = obj
        .filter((policy) => !policy.global)
        .map((policy) => normalizePolicy(policy));

      const changedGlobalPolicies = obj
        .filter((policy) => policy.global)
        .map((policy) => normalizePolicy(policy));

      try {
        await adminClient.clientPolicies.updatePolicy({
          policies: changedPolicies,
          globalPolicies: changedGlobalPolicies,
        });
        addAlert(t("updateClientPoliciesSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("updateClientPoliciesError", error);
      }
    } catch (error) {
      console.warn("Invalid json, ignoring value using {}");
      addError("invalidJsonClientPoliciesError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientPolicyConfirmTitle"),
    messageKey: t("deleteClientPolicyConfirm", {
      policyName: selectedPolicy?.name,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedPolicies = policies
        ?.filter((policy) => {
          return !policy.global && policy.name !== selectedPolicy?.name;
        })
        .map<ClientPolicyRepresentation>((policy) => {
          const newPolicy = { ...policy };
          delete newPolicy.global;
          return newPolicy;
        });

      try {
        await adminClient.clientPolicies.updatePolicy({
          policies: updatedPolicies,
        });
        addAlert(t("deleteClientPolicySuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("deleteClientPolicyError", error);
      }
    },
  });

  if (!policies) {
    return <KeycloakSpinner />;
  }
  return (
    <>
      <DeleteConfirm />
      <PageSection>
        <Flex className="kc-policies-config-section">
          <FlexItem>
            <Title headingLevel="h1" size="md">
              {t("policiesConfigType")}
            </Title>
          </FlexItem>
          <FlexItem>
            <Radio
              isChecked={!show}
              name="policiesView"
              onChange={() => setShow(false)}
              label={t("policiesConfigTypes.formView")}
              id="formView-policiesView"
              data-testid="formView-policiesView"
              className="kc-form-radio-btn pf-v5-u-mr-sm pf-v5-u-ml-sm"
            />
          </FlexItem>
          <FlexItem>
            <Radio
              isChecked={show}
              name="policiesView"
              onChange={() => setShow(true)}
              label={t("policiesConfigTypes.jsonEditor")}
              id="jsonEditor-policiesView"
              data-testid="jsonEditor-policiesView"
              className="kc-editor-radio-btn"
            />
          </FlexItem>
        </Flex>
      </PageSection>
      <Divider />
      {!show ? (
        <KeycloakDataTable
          key={policies.length}
          emptyState={
            <ListEmptyState
              message={t("noClientPolicies")}
              instructions={t("noClientPoliciesInstructions")}
              primaryActionText={t("createClientPolicy")}
              onPrimaryAction={() => navigate(toAddClientPolicy({ realm }))}
            />
          }
          ariaLabelKey="clientPolicies"
          searchPlaceholderKey="clientPolicySearch"
          loader={loader}
          toolbarItem={
            <ToolbarItem>
              <Button
                id="createPolicy"
                component={(props) => (
                  <Link {...props} to={toAddClientPolicy({ realm })} />
                )}
                data-testid="createPolicy"
              >
                {t("createClientPolicy")}
              </Button>
            </ToolbarItem>
          }
          isRowDisabled={(value) => !!value.global}
          actions={[
            {
              title: t("delete"),
              onRowClick: (item) => {
                toggleDeleteDialog();
                setSelectedPolicy(item);
              },
            } as Action<ClientPolicy>,
          ]}
          columns={[
            {
              name: "name",
              cellRenderer: ({ name }: ClientPolicyRepresentation) => (
                <Link to={toEditClientPolicy({ realm, policyName: name! })}>
                  {name}
                </Link>
              ),
            },
            {
              name: "enabled",
              displayKey: "status",
              cellRenderer: (clientPolicy) => (
                <SwitchRenderer
                  clientPolicy={clientPolicy}
                  form={form}
                  saveStatus={saveStatus}
                  onConfirm={async () => {
                    form.setValue(clientPolicy.name!, false);
                    await saveStatus();
                  }}
                />
              ),
            },
            {
              name: "description",
              cellFormatters: [translationFormatter(t)],
            },
          ]}
        />
      ) : (
        <>
          <div className="pf-v5-u-mt-md pf-v5-u-ml-lg">
            <CodeEditor
              value={code}
              language="json"
              onChange={(value) => setCode(value)}
              height={480}
            />
          </div>
          <div className="pf-v5-u-mt-md">
            <Button
              variant={ButtonVariant.primary}
              className="pf-v5-u-mr-md pf-v5-u-ml-lg"
              data-testid="jsonEditor-policies-saveBtn"
              onClick={save}
            >
              {t("save")}
            </Button>
            <Button
              variant={ButtonVariant.link}
              data-testid="jsonEditor-reloadBtn"
              onClick={() => {
                setCode(prettyPrintJSON(tablePolicies));
              }}
            >
              {t("reload")}
            </Button>
          </div>
        </>
      )}
    </>
  );
};

type SwitchRendererProps = {
  clientPolicy: ClientPolicy;
  form: UseFormReturn<Record<string, boolean>>;
  saveStatus: () => void;
  onConfirm: () => void;
};

const SwitchRenderer = ({
  clientPolicy,
  form,
  saveStatus,
  onConfirm,
}: SwitchRendererProps) => {
  const { t } = useTranslation();
  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "disablePolicyConfirmTitle",
    messageKey: "disablePolicyConfirm",
    continueButtonLabel: "disable",
    onConfirm,
  });

  return (
    <>
      <DisableConfirm />
      <Controller
        name={clientPolicy.name!}
        data-testid={`${clientPolicy.name!}-switch`}
        defaultValue={clientPolicy.enabled}
        control={form.control}
        render={({ field }) => (
          <Switch
            label={t("enabled")}
            labelOff={t("disabled")}
            isChecked={field.value}
            isDisabled={clientPolicy.global}
            onChange={(_event, value) => {
              if (!value) {
                toggleDisableDialog();
              } else {
                field.onChange(value);
                saveStatus();
              }
            }}
            aria-label={clientPolicy.name!}
          />
        )}
      />
    </>
  );
};
