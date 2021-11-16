import React, { useState } from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Flex,
  FlexItem,
  PageSection,
  Radio,
  Title,
  ToolbarItem,
} from "@patternfly/react-core";

import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useTranslation } from "react-i18next";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { prettyPrintJSON, upperCaseFormatter } from "../util";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import { Link, useHistory } from "react-router-dom";
import type ClientPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyRepresentation";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";

import "./RealmSettingsSection.css";
import { useRealm } from "../context/realm-context/RealmContext";
import { toAddClientPolicy } from "./routes/AddClientPolicy";
import { toEditClientPolicy } from "./routes/EditClientPolicy";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";

export const PoliciesTab = () => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const history = useHistory();
  const [show, setShow] = useState(false);
  const [policies, setPolicies] = useState<ClientPolicyRepresentation[]>();
  const [selectedPolicy, setSelectedPolicy] =
    useState<ClientPolicyRepresentation>();
  const [key, setKey] = useState(0);
  const [code, setCode] = useState<string>();
  const [tablePolicies, setTablePolicies] =
    useState<ClientPolicyRepresentation[]>();

  const refresh = () => setKey(key + 1);

  useFetch(
    () => adminClient.clientPolicies.listPolicies(),
    (policies) => {
      setPolicies(policies.policies),
        setTablePolicies(policies.policies || []),
        setCode(prettyPrintJSON(policies.policies));
    },
    [key]
  );

  const loader = async () => policies ?? [];

  const ClientPolicyDetailLink = ({ name }: ClientPolicyRepresentation) => (
    <Link to={toEditClientPolicy({ realm, policyName: name! })}>{name}</Link>
  );

  const save = async () => {
    if (!code) {
      return;
    }

    try {
      const obj: ClientPolicyRepresentation[] = JSON.parse(code);

      try {
        await adminClient.clientPolicies.updatePolicy({
          policies: obj,
        });
        addAlert(
          t("realm-settings:updateClientPoliciesSuccess"),
          AlertVariant.success
        );
        refresh();
      } catch (error) {
        addError("realm-settings:updateClientPoliciesError", error);
      }
    } catch (error) {
      console.warn("Invalid json, ignoring value using {}");
      addError("realm-settings:updateClientPoliciesError", error);
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
      const updatedPolicies = policies?.filter(
        (policy) => policy.name !== selectedPolicy?.name
      );

      try {
        await adminClient.clientPolicies.updatePolicy({
          policies: updatedPolicies,
        });
        addAlert(t("deleteClientPolicySuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError(t("deleteClientPolicyError"), error);
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
              className="kc-form-radio-btn pf-u-mr-sm pf-u-ml-sm"
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
              message={t("realm-settings:noClientPolicies")}
              instructions={t("realm-settings:noClientPoliciesInstructions")}
              primaryActionText={t("realm-settings:createClientPolicy")}
              onPrimaryAction={() => history.push(toAddClientPolicy({ realm }))}
            />
          }
          ariaLabelKey="realm-settings:clientPolicies"
          searchPlaceholderKey="realm-settings:clientPolicySearch"
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
          actions={[
            {
              title: t("common:delete"),
              onRowClick: (item) => {
                toggleDeleteDialog();
                setSelectedPolicy(item);
              },
            },
          ]}
          columns={[
            {
              name: "name",
              cellRenderer: ClientPolicyDetailLink,
            },
            {
              name: "enabled",
              cellFormatters: [upperCaseFormatter()],
            },
            {
              name: "description",
            },
          ]}
        />
      ) : (
        <>
          <div className="pf-u-mt-md pf-u-ml-lg">
            <CodeEditor
              isLineNumbersVisible
              isLanguageLabelVisible
              isReadOnly={false}
              code={code}
              language={Language.json}
              height="30rem"
              onChange={(value) => {
                setCode(value ?? "");
              }}
            />
          </div>
          <div className="pf-u-mt-md">
            <Button
              variant={ButtonVariant.primary}
              className="pf-u-mr-md pf-u-ml-lg"
              data-testid="jsonEditor-policies-saveBtn"
              onClick={save}
            >
              {t("save")}
            </Button>
            <Button
              variant={ButtonVariant.secondary}
              data-testid="jsonEditor-reloadBtn"
              onClick={() => {
                setCode(prettyPrintJSON(tablePolicies));
              }}
            >
              {" "}
              {t("reload")}
            </Button>
          </div>
        </>
      )}
    </>
  );
};
