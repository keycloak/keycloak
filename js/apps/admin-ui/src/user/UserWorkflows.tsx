import {
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionToggle,
  Label,
} from "@patternfly/react-core";
import yaml from "yaml";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import useFormatDate from "../utils/useFormatDate";
import WorkflowRepresentation from "libs/keycloak-admin-client/lib/defs/workflowRepresentation";
import CodeEditor from "../components/form/CodeEditor";

type UserWorkflowProps = {
  user?: string;
};

const WorkflowYAMLAccordion = ({ id }: { id: string }) => {
  const [expanded, setExpanded] = useState(false);
  const [yamlContent, setYamlContent] = useState<string>("");
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();

  const onToggleWorkflowYaml = () => {
    if (expanded) {
      setExpanded(false);
    } else {
      setExpanded(true);
      void loadWorkflowYaml();
    }
  };

  const loadWorkflowYaml = async () => {
    const workflowYaml = await adminClient.workflows.findOne({
      id: id!,
      includeId: false,
    });
    setYamlContent(yaml.stringify(workflowYaml));
  };

  return (
    <Accordion asDefinitionList={true} isBordered togglePosition="start">
      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggleWorkflowYaml();
          }}
          isExpanded={expanded}
          id={`yaml-ex-toggle1-${id}`}
        >
          {t("workflowYAML")}
        </AccordionToggle>
        <AccordionContent id={`ex-expand1-content-${id}`} isHidden={!expanded}>
          <CodeEditor
            id={`workflowYAML-${id}`}
            data-testid={`workflowYAML-${id}`}
            value={yamlContent}
            language="yaml"
            readOnly={true}
            height={300}
          />
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
};

const StepsCell = (workflow: WorkflowRepresentation) => {
  const formatDate = useFormatDate();
  const { t } = useTranslation();

  return (
    <>
      <Table>
        <Thead>
          <Tr>
            <Th>{t("step")}</Th>
            <Th>{t("scheduledAt")}</Th>
            <Th>{t("status")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {workflow.steps?.map((step, idx) => (
            <Tr key={idx}>
              <Td>{step.uses}</Td>
              <Td>
                {step["scheduled-at"]
                  ? formatDate(new Date(step["scheduled-at"]!))
                  : t("immediateStep")}
              </Td>
              <Td>
                {step.status! === "COMPLETED" ? (
                  <Label color="green">{t("completed")}</Label>
                ) : (
                  <Label color="orange">{t("pending")}</Label>
                )}
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
      <WorkflowYAMLAccordion id={workflow.id!} />
    </>
  );
};

export const UserWorkflows = ({ user }: UserWorkflowProps) => {
  const [key, setKey] = useState(0);
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();

  const workflowsLoader = async () => {
    return adminClient.workflows.scheduled({
      userId: user!,
    });
  };

  return (
    <KeycloakDataTable
      key={key}
      loader={workflowsLoader}
      ariaLabelKey="titleWorkflows"
      columns={[
        { name: "name", displayKey: "name" },
        { name: "on", displayKey: "on" },
        {
          name: "steps",
          displayKey: "steps",
          cellRenderer: (row) => row.steps!.length.toString(),
        },
      ]}
      detailColumns={[
        {
          name: "details",
          enabled: () => {
            return true;
          },
          cellRenderer: StepsCell,
        },
      ]}
      isPaginated={false}
      searchPlaceholderKey="searchByName"
      emptyState={
        <ListEmptyState
          message={t("emptyWorkflows")}
          instructions={t("emptyUserWorkflowsInstructions")}
          primaryActionText={t("refresh")}
          onPrimaryAction={() => setKey(key + 1)}
        />
      }
    />
  );
};
