import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  ExpandableSection,
  FormGroup,
  InputGroup,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextInput,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type GlobalRequestResult from "@keycloak/keycloak-admin-client/lib/defs/globalRequestResult";
import moment from "moment";
import React, { useEffect, useRef, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { convertToFormValues, toUpperCase } from "../util";
import { AddHostDialog } from "./advanced/AddHostDialog";
import { AdvancedSettings } from "./advanced/AdvancedSettings";
import { AuthenticationOverrides } from "./advanced/AuthenticationOverrides";
import { FineGrainOpenIdConnect } from "./advanced/FineGrainOpenIdConnect";
import { FineGrainSamlEndpointConfig } from "./advanced/FineGrainSamlEndpointConfig";
import { OpenIdConnectCompatibilityModes } from "./advanced/OpenIdConnectCompatibilityModes";
import type { SaveOptions } from "./ClientDetails";
import { toClient } from "./routes/Client";

type AdvancedProps = {
  save: (options?: SaveOptions) => void;
  client: ClientRepresentation;
};

export const AdvancedTab = ({
  save,
  client: {
    id,
    registeredNodes,
    attributes,
    protocol,
    authenticationFlowBindingOverrides,
    adminUrl,
  },
}: AdvancedProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const revocationFieldName = "notBefore";
  const openIdConnect = "openid-connect";

  const { getValues, setValue, register, control, reset } = useFormContext();
  const [expanded, setExpanded] = useState(false);
  const [selectedNode, setSelectedNode] = useState("");
  const [addNodeOpen, setAddNodeOpen] = useState(false);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const [nodes, setNodes] = useState(registeredNodes || {});
  const pushRevocationButtonRef = useRef<HTMLElement>();

  const setNotBefore = (time: number, messageKey: string) => {
    setValue(revocationFieldName, time);
    save({ messageKey });
  };

  const parseResult = (result: GlobalRequestResult, prefixKey: string) => {
    const successCount = result.successRequests?.length || 0;
    const failedCount = result.failedRequests?.length || 0;

    if (successCount === 0 && failedCount === 0) {
      addAlert(t("noAdminUrlSet"), AlertVariant.warning);
    } else if (failedCount > 0) {
      addAlert(
        t(prefixKey + "Success", { successNodes: result.successRequests }),
        AlertVariant.success
      );
      addAlert(
        t(prefixKey + "Fail", { failedNodes: result.failedRequests }),
        AlertVariant.danger
      );
    } else {
      addAlert(
        t(prefixKey + "Success", { successNodes: result.successRequests }),
        AlertVariant.success
      );
    }
  };

  const resetFields = (names: string[]) => {
    const values: { [name: string]: string } = {};
    for (const name of names) {
      values[`attributes.${name}`] = attributes?.[name];
    }
    reset(values);
  };

  const push = async () => {
    const result = (await adminClient.clients.pushRevocation({
      id: id!,
    })) as unknown as GlobalRequestResult;
    parseResult(result, "notBeforePush");
  };

  const testCluster = async () => {
    const result = await adminClient.clients.testNodesAvailable({ id: id! });
    parseResult(result, "testCluster");
  };

  const [toggleDeleteNodeConfirm, DeleteNodeConfirm] = useConfirmDialog({
    titleKey: "clients:deleteNode",
    messageKey: t("deleteNodeBody", {
      node: selectedNode,
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clients.deleteClusterNode({
          id: id!,
          node: selectedNode,
        });
        setNodes({
          ...Object.keys(nodes).reduce((object: any, key) => {
            if (key !== selectedNode) {
              object[key] = nodes[key];
            }
            return object;
          }, {}),
        });
        refresh();
        addAlert(t("deleteNodeSuccess"), AlertVariant.success);
      } catch (error) {
        addError("clients:deleteNodeFail", error);
      }
    },
  });

  useEffect(() => {
    register(revocationFieldName);
  }, [register]);

  const formatDate = () => {
    const date = getValues(revocationFieldName);
    if (date > 0) {
      return moment(date * 1000).format("LLL");
    } else {
      return t("common:none");
    }
  };

  const sections = [
    t("revocation"),
    t("clustering"),
    protocol === openIdConnect
      ? t("fineGrainOpenIdConnectConfiguration")
      : t("fineGrainSamlEndpointConfig"),
    t("advancedSettings"),
    t("authenticationOverrides"),
  ];
  if (protocol === openIdConnect) {
    sections.splice(3, 0, t("openIdConnectCompatibilityModes"));
  }

  return (
    <PageSection variant="light" className="pf-u-py-0">
      <ScrollForm sections={sections} borders>
        <>
          <Text className="pf-u-pb-lg">
            <Trans i18nKey="clients-help:notBeforeIntro">
              In order to successfully push setup url on
              <Link to={toClient({ realm, clientId: id!, tab: "settings" })}>
                {t("settings")}
              </Link>
              tab
            </Trans>
          </Text>
          <FormAccess role="manage-clients" isHorizontal>
            <FormGroup
              label={t("notBefore")}
              fieldId="kc-not-before"
              labelIcon={
                <HelpItem
                  helpText="clients-help:notBefore"
                  fieldLabelId="clients:notBefore"
                />
              }
            >
              <InputGroup>
                <TextInput
                  type="text"
                  id="kc-not-before"
                  name="notBefore"
                  isReadOnly
                  value={formatDate()}
                />
                <Button
                  id="setToNow"
                  variant="control"
                  onClick={() => {
                    setNotBefore(moment.now() / 1000, "notBeforeSetToNow");
                  }}
                >
                  {t("setToNow")}
                </Button>
                <Button
                  id="clear"
                  variant="control"
                  onClick={() => {
                    setNotBefore(0, "notBeforeNowClear");
                  }}
                >
                  {t("clear")}
                </Button>
              </InputGroup>
            </FormGroup>
            <ActionGroup>
              {!adminUrl && (
                <Tooltip
                  reference={pushRevocationButtonRef}
                  content={t("clients-help:notBeforeTooltip")}
                />
              )}
              <Button
                id="push"
                variant="secondary"
                onClick={push}
                isAriaDisabled={!adminUrl}
                ref={pushRevocationButtonRef}
              >
                {t("push")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </>
        <>
          <FormAccess role="manage-clients" isHorizontal>
            <FormGroup
              label={t("nodeReRegistrationTimeout")}
              fieldId="kc-node-reregistration-timeout"
              labelIcon={
                <HelpItem
                  helpText="clients-help:nodeReRegistrationTimeout"
                  fieldLabelId="clients:nodeReRegistrationTimeout"
                />
              }
            >
              <Split hasGutter>
                <SplitItem>
                  <Controller
                    name="nodeReRegistrationTimeout"
                    defaultValue=""
                    control={control}
                    render={({ onChange, value }) => (
                      <TimeSelector value={value} onChange={onChange} />
                    )}
                  />
                </SplitItem>
                <SplitItem>
                  <Button
                    variant={ButtonVariant.secondary}
                    onClick={() => save()}
                  >
                    {t("common:save")}
                  </Button>
                </SplitItem>
              </Split>
            </FormGroup>
          </FormAccess>
          <>
            <DeleteNodeConfirm />
            <AddHostDialog
              clientId={id!}
              isOpen={addNodeOpen}
              onAdded={(node) => {
                nodes[node] = moment.now() / 1000;
                refresh();
              }}
              onClose={() => setAddNodeOpen(false)}
            />
            <ExpandableSection
              toggleText={t("registeredClusterNodes")}
              onToggle={setExpanded}
              isExpanded={expanded}
            >
              <KeycloakDataTable
                key={key}
                ariaLabelKey="registeredClusterNodes"
                loader={() =>
                  Promise.resolve(
                    Object.entries(nodes || {}).map((entry) => {
                      return { host: entry[0], registration: entry[1] };
                    })
                  )
                }
                toolbarItem={
                  <>
                    <ToolbarItem>
                      <Button
                        id="testClusterAvailability"
                        onClick={testCluster}
                        variant={ButtonVariant.secondary}
                        isDisabled={Object.keys(nodes).length === 0}
                      >
                        {t("testClusterAvailability")}
                      </Button>
                    </ToolbarItem>
                    <ToolbarItem>
                      <Button
                        id="registerNodeManually"
                        onClick={() => setAddNodeOpen(true)}
                        variant={ButtonVariant.tertiary}
                      >
                        {t("registerNodeManually")}
                      </Button>
                    </ToolbarItem>
                  </>
                }
                actions={[
                  {
                    title: t("common:delete"),
                    onRowClick: (node) => {
                      setSelectedNode(node.host);
                      toggleDeleteNodeConfirm();
                    },
                  },
                ]}
                columns={[
                  {
                    name: "host",
                    displayKey: "clients:nodeHost",
                  },
                  {
                    name: "registration",
                    displayKey: "clients:lastRegistration",
                    cellFormatters: [
                      (value) =>
                        value
                          ? moment(parseInt(value.toString()) * 1000).format(
                              "LLL"
                            )
                          : "",
                    ],
                  },
                ]}
                emptyState={
                  <ListEmptyState
                    message={t("noNodes")}
                    instructions={t("noNodesInstructions")}
                    primaryActionText={t("registerNodeManually")}
                    onPrimaryAction={() => setAddNodeOpen(true)}
                  />
                }
              />
            </ExpandableSection>
          </>
        </>
        <>
          {protocol === openIdConnect && (
            <>
              <Text className="pf-u-pb-lg">
                {t("clients-help:fineGrainOpenIdConnectConfiguration")}
              </Text>
              <FineGrainOpenIdConnect
                control={control}
                save={() => save()}
                reset={() =>
                  convertToFormValues(attributes, (key, value) =>
                    setValue(`attributes.${key}`, value)
                  )
                }
              />
            </>
          )}
          {protocol !== openIdConnect && (
            <>
              <Text className="pf-u-pb-lg">
                {t("clients-help:fineGrainSamlEndpointConfig")}
              </Text>
              <FineGrainSamlEndpointConfig
                control={control}
                save={() => save()}
                reset={() =>
                  convertToFormValues(attributes, (key, value) =>
                    setValue(`attributes.${key}`, value)
                  )
                }
              />
            </>
          )}
        </>
        {protocol === openIdConnect && (
          <>
            <Text className="pf-u-pb-lg">
              {t("clients-help:openIdConnectCompatibilityModes")}
            </Text>
            <OpenIdConnectCompatibilityModes
              control={control}
              save={() => save()}
              reset={() =>
                resetFields(["exclude.session.state.from.auth.response"])
              }
            />
          </>
        )}
        <>
          <Text className="pf-u-pb-lg">
            {t("clients-help:advancedSettings" + toUpperCase(protocol || ""))}
          </Text>
          <AdvancedSettings
            protocol={protocol}
            control={control}
            save={() => save()}
            reset={() => {
              resetFields([
                "saml.assertion.lifespan",
                "access.token.lifespan",
                "tls.client.certificate.bound.access.tokens",
                "pkce.code.challenge.method",
              ]);
            }}
          />
        </>
        <>
          <Text className="pf-u-pb-lg">
            {t("clients-help:authenticationOverrides")}
          </Text>
          <AuthenticationOverrides
            protocol={protocol}
            control={control}
            save={() => save()}
            reset={() => {
              setValue(
                "authenticationFlowBindingOverrides.browser",
                authenticationFlowBindingOverrides?.browser
              );
              setValue(
                "authenticationFlowBindingOverrides.direct_grant",
                authenticationFlowBindingOverrides?.direct_grant
              );
            }}
          />
        </>
      </ScrollForm>
    </PageSection>
  );
};
