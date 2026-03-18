import {
  AlertVariant,
  Button,
  ButtonVariant,
  ExpandableSection,
  FormGroup,
  Split,
  SplitItem,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { TimeSelectorForm } from "../../components/time-selector/TimeSelectorForm";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../../utils/useFormatDate";
import { AddHostDialog } from ".././advanced/AddHostDialog";
import { AdvancedProps, parseResult } from "../AdvancedTab";

type Node = {
  host: string;
  registration: string;
};

export const ClusteringPanel = ({
  save,
  client: { id, registeredNodes, access },
}: AdvancedProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();

  const [nodes, setNodes] = useState(registeredNodes || {});
  const [expanded, setExpanded] = useState(false);
  const [selectedNode, setSelectedNode] = useState("");
  const [addNodeOpen, setAddNodeOpen] = useState(false);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const testCluster = async () => {
    const result = await adminClient.clients.testNodesAvailable({ id: id! });
    parseResult(result, "testCluster", addAlert, t);
  };

  const [toggleDeleteNodeConfirm, DeleteNodeConfirm] = useConfirmDialog({
    titleKey: "deleteNode",
    messageKey: t("deleteNodeBody", {
      node: selectedNode,
    }),
    continueButtonLabel: "delete",
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
        addError("deleteNodeFail", error);
      }
    },
  });

  return (
    <>
      <FormAccess
        role="manage-clients"
        fineGrainedAccess={access?.configure}
        isHorizontal
      >
        <FormGroup
          label={t("nodeReRegistrationTimeout")}
          fieldId="kc-node-reregistration-timeout"
          labelIcon={
            <HelpItem
              helpText={t("nodeReRegistrationTimeoutHelp")}
              fieldLabelId="nodeReRegistrationTimeout"
            />
          }
        >
          <Split hasGutter>
            <SplitItem>
              <TimeSelectorForm name="nodeReRegistrationTimeout" />
            </SplitItem>
            <SplitItem>
              <Button variant={ButtonVariant.secondary} onClick={() => save()}>
                {t("save")}
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
            nodes[node] = Date.now() / 1000;
            refresh();
          }}
          onClose={() => setAddNodeOpen(false)}
        />
        <ExpandableSection
          toggleText={t("registeredClusterNodes")}
          onToggle={(_event, val) => setExpanded(val)}
          isExpanded={expanded}
        >
          <KeycloakDataTable
            key={key}
            ariaLabelKey="registeredClusterNodes"
            loader={() =>
              Promise.resolve<Node[]>(
                Object.entries(nodes || {}).map((entry) => {
                  return { host: entry[0], registration: entry[1] };
                }),
              )
            }
            toolbarItem={
              <>
                <ToolbarItem>
                  <Button
                    id="testClusterAvailability"
                    data-testid="test-cluster-availability"
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
                    data-testid="registerNodeManually"
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
                title: t("delete"),
                onRowClick: (node) => {
                  setSelectedNode(node.host);
                  toggleDeleteNodeConfirm();
                },
              } as Action<Node>,
            ]}
            columns={[
              {
                name: "host",
                displayKey: "nodeHost",
              },
              {
                name: "registration",
                displayKey: "lastRegistration",
                cellFormatters: [
                  (value) =>
                    value
                      ? formatDate(
                          new Date(parseInt(value.toString()) * 1000),
                          FORMAT_DATE_AND_TIME,
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
  );
};
