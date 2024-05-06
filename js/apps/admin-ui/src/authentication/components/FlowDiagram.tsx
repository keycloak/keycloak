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

type ConditionLabel = "true" | "false";

const nodeTypes = {
  conditional: ConditionalNode,
  startSubFlow: StartSubFlowNode,
  endSubFlow: EndSubFlowNode,
} as const;

type NodeType = keyof typeof nodeTypes;

const isBypassable = (execution: ExpandableExecution) =>
  execution.requirement === "ALTERNATIVE" ||
  execution.requirement === "DISABLED";

const createEdge = (
  fromNode: string,
  toNode: string,
  label?: ConditionLabel,
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

const renderSubFlowNodes = (execution: ExpandableExecution): Node[] => {
  const nodes: Node[] = [];

  if (execution.requirement !== "CONDITIONAL") {
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
  }

  return nodes.concat(renderFlowNodes(execution.executionList || []));
};

const renderFlowNodes = (executionList: ExpandableExecution[]): Node[] => {
  let elements: Node[] = [];

  for (let index = 0; index < executionList.length; index++) {
    const execution = executionList[index];
    if (execution.executionList) {
      elements = elements.concat(renderSubFlowNodes(execution));
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

const renderSubFlowEdges = (
  execution: ExpandableExecution,
  flowEndId: string,
): { startId: string; edges: Edge[]; endId: string } => {
  if (!execution.executionList)
    throw new Error("Execution list is required for subflow");

  if (execution.requirement === "CONDITIONAL") {
    const startId = execution.executionList![0].id!;

    return {
      startId: startId,
      edges: renderFlowEdges(startId, execution.executionList!, flowEndId),
      endId: execution.executionList![execution.executionList!.length - 1].id!,
    };
  }
  const elements: Edge[] = [];
  const subFlowEndId = `flow-end-${execution.id}`;

  return {
    startId: execution.id!,
    edges: elements.concat(
      renderFlowEdges(execution.id!, execution.executionList!, subFlowEndId),
    ),
    endId: subFlowEndId,
  };
};

const renderFlowEdges = (
  startId: string,
  executionList: ExpandableExecution[],
  endId: string,
): Edge[] => {
  let elements: Edge[] = [];
  let prevExecutionId = startId;
  let isLastExecutionBypassable = false;
  const conditionals = [];

  for (let index = 0; index < executionList.length; index++) {
    const execution = executionList[index];
    let executionId = execution.id!;
    const isPrevConditional =
      conditionals[conditionals.length - 1] === prevExecutionId;
    const connectToPrevious = (id: string) =>
      elements.push(
        createEdge(prevExecutionId, id, isPrevConditional ? "true" : undefined),
      );

    if (providerConditionFilter(execution)) {
      conditionals.push(executionId);
    }
    if (startId === executionId) {
      continue;
    }

    if (execution.executionList) {
      const nextRequired =
        executionList.slice(index + 1).find((e) => !isBypassable(e))?.id ??
        endId;
      const {
        startId: subFlowStartId,
        edges,
        endId: subflowEndId,
      } = renderSubFlowEdges(execution, nextRequired);

      connectToPrevious(subFlowStartId);
      elements = elements.concat(edges);
      executionId = subflowEndId;
    } else {
      connectToPrevious(executionId);
    }

    const isExecutionBypassable = isBypassable(execution);

    if (isExecutionBypassable) {
      elements.push(createEdge(executionId, endId));
    } else {
      prevExecutionId = executionId;
    }

    isLastExecutionBypassable = isExecutionBypassable;
  }

  // subflows with conditionals automatically connect to the end, so don't do it twice
  if (!isLastExecutionBypassable && conditionals.length === 0) {
    elements.push(createEdge(prevExecutionId, endId));
  }
  elements = elements.concat(
    conditionals.map((id) => createEdge(id, endId, "false")),
  );

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
  return getLayoutedEdges(renderFlowEdges("start", expandableList, "end"));
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
