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
import { Controller, useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useAdminClient } from "../../context/auth/AdminClient";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../../utils/useFormatDate";
import { AddHostDialog } from ".././advanced/AddHostDialog";
import { AdvancedProps, parseResult } from "../AdvancedTab";

export const ClusteringPanel = ({
  save,
  client: { id, registeredNodes, access },
}: AdvancedProps) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();
  const { adminClient } = useAdminClient();
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
                render={({ field }) => (
                  <TimeSelector value={field.value} onChange={field.onChange} />
                )}
              />
            </SplitItem>
            <SplitItem>
              <Button variant={ButtonVariant.secondary} onClick={() => save()}>
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
            nodes[node] = Date.now() / 1000;
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
                      ? formatDate(
                          new Date(parseInt(value.toString()) * 1000),
                          FORMAT_DATE_AND_TIME
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
