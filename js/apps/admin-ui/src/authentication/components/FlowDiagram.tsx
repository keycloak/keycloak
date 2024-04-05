import type AuthenticationExecutionInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationExecutionInfoRepresentation";
import { MouseEvent as ReactMouseEvent, useMemo, useState } from "react";
import {
  Background,
  Controls,
  Edge,
  EdgeTypes,
  MiniMap,
  Node,
  NodeMouseHandler,
  Position,
  ReactFlow,
  ReactFlowInstance,
  useEdgesState,
  useNodesState,
} from "reactflow";
import { useUpdateEffect } from "../../utils/useUpdateEffect";

import type { ExecutionList, ExpandableExecution } from "../execution-model";
import { providerConditionFilter } from "../FlowDetails";
import { getLayoutedEdges, getLayoutedNodes } from "./diagram/auto-layout";
import { ButtonEdge, ButtonEdges } from "./diagram/ButtonEdge";
import { ConditionalNode } from "./diagram/ConditionalNode";
import { EndSubFlowNode, StartSubFlowNode } from "./diagram/SubFlowNode";

import "reactflow/dist/style.css";
import "./flow-diagram.css";

type FlowDiagramProps = {
  executionList: ExecutionList;
};


const nodeTypes = {
  conditional: ConditionalNode,
  startSubFlow: StartSubFlowNode,
  endSubFlow: EndSubFlowNode,
} as const;

type NodeType = keyof typeof nodeTypes;

const createEdge = (
  fromNode: string,
  toNode: string,
  label?: string,
): Edge => ({
  id: `edge-${fromNode}-to-${toNode}`,
  type: "buttonEdge",
  source: fromNode,
  target: toNode,
  label: label,
  data: {
    onEdgeClick: (
      evt: ReactMouseEvent<HTMLButtonElement, MouseEvent>,
      id: string,
    ) => {
      evt.stopPropagation();
      alert(`hello ${id}`);
    },
  },
});

const createNode = (
  ex: { id?: string; displayName?: string },
  nodeType?: NodeType,
): Node => {
  return {
    id: ex.id!,
    type: nodeType,
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    data: { label: ex.displayName! },
    position: { x: 0, y: 0 },
  };
};

const renderParallelEdges = (
  start: AuthenticationExecutionInfoRepresentation,
  execution: ExpandableExecution,
  end: AuthenticationExecutionInfoRepresentation,
): Edge[] => {
  const falseConditionLabel = providerConditionFilter(execution) ? "false" : "";
  return [
    createEdge(start.id!, execution.id!),
    createEdge(execution.id!, end.id!, falseConditionLabel),
  ];
};

const renderSequentialEdges = (
  start: AuthenticationExecutionInfoRepresentation,
  execution: ExpandableExecution,
  end: AuthenticationExecutionInfoRepresentation,
  prefExecution: ExpandableExecution,
  isFirst: boolean,
  isLast: boolean,
): Edge[] => {
  const edges: Edge[] = [];

  if (isFirst) {
    edges.push(createEdge(start.id!, execution.id!));
  } else {
    const trueConditionLabel = providerConditionFilter(prefExecution)
      ? "true"
      : "";
    edges.push(
      createEdge(prefExecution.id!, execution.id!, trueConditionLabel),
    );
  }

  if (isLast || providerConditionFilter(execution)) {
    const falseConditionLabel = providerConditionFilter(execution)
      ? "false"
      : "";
    edges.push(createEdge(execution.id!, end.id!, falseConditionLabel));
  }

  return edges;
};

const renderConditionalSubFlowNodes = (
  execution: ExpandableExecution,
): Node[] => renderFlowNodes(execution.executionList || []);

const renderConditionalSubFlowEdges = (
  execution: ExpandableExecution,
  start: AuthenticationExecutionInfoRepresentation,
  end: AuthenticationExecutionInfoRepresentation,
  prefExecution?: ExpandableExecution,
): Edge[] => {
  const conditionalSubFlowStart =
    prefExecution && prefExecution.requirement !== "ALTERNATIVE"
      ? prefExecution
      : start!;

  return renderFlowEdges(
    conditionalSubFlowStart,
    execution.executionList || [],
    end,
  );
};

const renderSubFlowNodes = (execution: ExpandableExecution): Node[] => {
  const nodes: Node[] = [];

  nodes.push(createNode(execution, "startSubFlow"));

  const endSubFlowId = `flow-end-${execution.id}`;
  nodes.push(
    createNode(
      {
        id: endSubFlowId,
        displayName: execution.displayName!,
      },
      "endSubFlow",
    ),
  );

  return nodes.concat(renderFlowNodes(execution.executionList || []));
};

const renderSubFlowEdges = (
  execution: ExpandableExecution,
  start: AuthenticationExecutionInfoRepresentation,
  end: AuthenticationExecutionInfoRepresentation,
  prefExecution?: ExpandableExecution,
): Edge[] => {
  const edges: Edge[] = [];

  const endSubFlowId = `flow-end-${execution.id}`;

  edges.push(
    createEdge(
      prefExecution && prefExecution.requirement !== "ALTERNATIVE"
        ? prefExecution.id!
        : start.id!,
      execution.id!,
    ),
  );
  edges.push(createEdge(endSubFlowId, end.id!));

  return edges.concat(
    renderFlowEdges(execution, execution.executionList || [], {
      ...execution,
      id: endSubFlowId,
    }),
  );
};

const renderFlowNodes = (executionList: ExpandableExecution[]): Node[] => {
  let elements: Node[] = [];

  for (let index = 0; index < executionList.length; index++) {
    const execution = executionList[index];
    if (execution.executionList) {
      if (execution.requirement === "CONDITIONAL") {
        elements = elements.concat(renderConditionalSubFlowNodes(execution));
      } else {
        elements = elements.concat(renderSubFlowNodes(execution));
      }
    } else {
      elements.push(
        createNode(
          execution,
          providerConditionFilter(execution) ? "conditional" : undefined,
        ),
      );
    }
  }

  return elements;
};

const renderFlowEdges = (
  start: AuthenticationExecutionInfoRepresentation,
  executionList: ExpandableExecution[],
  end: AuthenticationExecutionInfoRepresentation,
): Edge[] => {
  let elements: Edge[] = [];

  for (let index = 0; index < executionList.length; index++) {
    const execution = executionList[index];
    if (execution.executionList) {
      if (execution.requirement === "CONDITIONAL") {
        elements = elements.concat(
          renderConditionalSubFlowEdges(
            execution,
            start,
            end,
            executionList[index - 1],
          ),
        );
      } else {
        elements = elements.concat(
          renderSubFlowEdges(execution, start, end, executionList[index - 1]),
        );
      }
    } else {
      if (
        execution.requirement === "ALTERNATIVE" ||
        execution.requirement === "DISABLED"
      ) {
        elements = elements.concat(renderParallelEdges(start, execution, end));
      } else {
        elements = elements.concat(
          renderSequentialEdges(
            start,
            execution,
            end,
            executionList[index - 1],
            index === 0,
            index === executionList.length - 1,
          ),
        );
      }
    }
  }

  return elements;
};

const edgeTypes: ButtonEdges = {
  buttonEdge: ButtonEdge,
};

function renderNodes(expandableList: ExpandableExecution[]) {
  return getLayoutedNodes([
    {
      id: "start",
      sourcePosition: Position.Right,
      type: "input",
      data: { label: "Start" },
      position: { x: 0, y: 0 },
      className: "keycloak__authentication__input_node",
    },
    {
      id: "end",
      targetPosition: Position.Left,
      type: "output",
      data: { label: "End" },
      position: { x: 0, y: 0 },
      className: "keycloak__authentication__output_node",
    },
    ...renderFlowNodes(expandableList),
  ]);
}

function renderEdges(expandableList: ExpandableExecution[]): Edge[] {
  return getLayoutedEdges(
    renderFlowEdges({ id: "start" }, expandableList, {
      id: "end",
    }),
  );
}

export const FlowDiagram = ({
  executionList: { expandableList },
}: FlowDiagramProps) => {
  const [expandDrawer, setExpandDrawer] = useState(false);
  const initialNodes = useMemo(() => renderNodes(expandableList), []);
  const initialEdges = useMemo(() => renderEdges(expandableList), []);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  useUpdateEffect(() => {
    setNodes(renderNodes(expandableList));
    setEdges(renderEdges(expandableList));
  }, [expandableList]);

  const onInit = (reactFlowInstance: ReactFlowInstance) =>
    reactFlowInstance.fitView({ duration: 100 });

  const onNodeClick: NodeMouseHandler = () => {
    setExpandDrawer(!expandDrawer);
  };

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onInit={onInit}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes as EdgeTypes}
      onNodeClick={onNodeClick}
      nodesConnectable={false}
    >
      <MiniMap />
      <Controls />
      <Background />
    </ReactFlow>
  );
};
