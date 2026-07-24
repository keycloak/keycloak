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
import { Popover, Button } from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import useFormatDate from "../utils/useFormatDate";
import WorkflowRepresentation from "libs/keycloak-admin-client/lib/defs/workflowRepresentation";
import CodeEditor from "../components/form/CodeEditor";

type UserWorkflowProps = {
  user?: string;
};

const WorkflowYAMLAccordion = ({ id, name }: { id: string; name: string }) => {
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
          id={`yaml-ex-toggle-${name}`}
          data-testid={`yaml-ex-toggle-${name}`}
        >
          {t("workflowYAML")}
        </AccordionToggle>
        <AccordionContent id={`ex-expand1-content-${id}`} isHidden={!expanded}>
          <CodeEditor
            id={`workflowYAML-${name}`}
            data-testid={`workflowYAML-${name}`}
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
      <Table aria-label={workflow.name! + "-" + t("steps")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("step")}</Th>
            <Th>{t("scheduledAfter")}</Th>
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
                  : ""}
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
      <WorkflowYAMLAccordion id={workflow.id!} name={workflow.name!} />
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

  const nextStepRenderer = (workflow: WorkflowRepresentation) => {
    if (!workflow.steps) return "";
    const found = workflow.steps.find((step) => step.status === "PENDING");
    if (found) {
      return found.uses;
    } else {
      return t("completed");
    }
  };

  return (
    <KeycloakDataTable
      key={key}
      loader={workflowsLoader}
      ariaLabelKey="titleWorkflows"
      columns={[
        { name: "name", displayKey: "name" },
        {
          name: "nextStep",
          displayKey: "nextStep",
          cellRenderer: (row) => nextStepRenderer(row) || "",
        },
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
      toolbarItem={[
        <Popover
          key="who-will-appear-popover"
          aria-label={t("whichWorkflowsWillAppear")}
          position="bottom"
          bodyContent={<div>{t("whichWorkflowsWillAppearDetail")}</div>}
        >
          <Button
            variant="link"
            className="kc-who-will-appear-button"
            key="who-will-appear-button"
            icon={<QuestionCircleIcon />}
          >
            {t("whichWorkflowsWillAppear")}
          </Button>
        </Popover>,
      ]}
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
