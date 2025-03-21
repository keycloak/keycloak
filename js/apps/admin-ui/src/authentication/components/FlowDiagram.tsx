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

type ConditionLabel = "true" | "false" | "success" | "attempted";

const nodeTypes = {
  conditional: ConditionalNode,
  startSubFlow: StartSubFlowNode,
  endSubFlow: EndSubFlowNode,
};

const inOutClasses = new Map<string, string>([
  ["input", "keycloak__authentication__input_node"],
  ["output", "keycloak__authentication__output_node"],
]);

type NodeType =
  | "conditional"
  | "startSubFlow"
  | "endSubFlow"
  | "input"
  | "output";

type IntermediateFlowResult = {
  startId: string;
  nodes: Node[];
  edges: Edge[];
  nextLinkFns: ((id: string) => Edge)[];
};

function pairwise<T, U>(fn: (x: T, y: T) => U, arr: T[]): U[] {
  const result: U[] = [];
  for (let index = 0; index < arr.length - 1; index++) {
    result.push(fn(arr[index], arr[index + 1]));
  }
  return result;
}

const isBypassable = (execution: ExpandableExecution) =>
  execution.requirement === "ALTERNATIVE" ||
  execution.requirement === "CONDITIONAL";

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
    sourcePosition: nodeType === "output" ? undefined : Position.Right,
    targetPosition: nodeType === "input" ? undefined : Position.Left,
    data: { label: ex.displayName! },
    position: { x: 0, y: 0 },
    className: inOutClasses.get(nodeType || ""),
  };
};

const consecutiveBypassableFlows = (
  executionList: ExpandableExecution[],
): ExpandableExecution[] => {
  const result = [];
  for (let index = 0; index < executionList.length; index++) {
    const execution = executionList[index];
    if (!isBypassable(execution)) {
      break;
    }
    result.push(execution);
  }
  return result;
};

const borderStep = (
  node: Node,
  continuing: boolean = true,
): IntermediateFlowResult => ({
  startId: node.id,
  nodes: [node],
  edges: [],
  nextLinkFns: continuing ? [(id: string) => createEdge(node.id, id)] : [],
});

const renderSubFlow = (
  execution: ExpandableExecution,
): IntermediateFlowResult => {
  if (!execution.executionList)
    throw new Error("Execution list is required for subflow");

  const graph = createGraph(createConcurrentGroupings(execution.executionList));

  graph.nextLinkFns.push(
    ...execution.executionList
      .filter((e) => providerConditionFilter(e))
      .map((e) => (id: string) => createEdge(e.id!, id, "false")),
  );
  return graph;
};

const groupConcurrentSteps = (
  executionList: ExpandableExecution[],
): ExpandableExecution[] => {
  const executions = consecutiveBypassableFlows(executionList);
  if (executions.length > 0) {
    return executions;
  }
  return [executionList[0]];
};

const createConcurrentSteps = (
  executionList: ExpandableExecution[],
): IntermediateFlowResult[] => {
  if (executionList.length === 0) {
    return [];
  }

  const executions = groupConcurrentSteps(executionList);
  return executions.map((execution) => {
    if (execution.executionList) {
      return renderSubFlow(execution);
    }

    const isConditional = providerConditionFilter(execution);
    const edgeLabel = (() => {
      if (isConditional) {
        return "true";
      }
      if (execution.requirement === "ALTERNATIVE") {
        return "success";
      }
    })();

    return {
      startId: execution.id!,
      nodes: [createNode(execution, isConditional ? "conditional" : undefined)],
      edges: [],
      nextLinkFns: [(id: string) => createEdge(execution.id!, id, edgeLabel)],
    };
  });
};

const createConcurrentGroupings = (
  executionList: ExpandableExecution[],
): IntermediateFlowResult[][] => {
  if (executionList.length === 0) {
    return [];
  }
  const steps = createConcurrentSteps(executionList);
  return [
    steps,
    ...createConcurrentGroupings(executionList.slice(steps.length)),
  ];
};

const createGraph = (
  groupings: IntermediateFlowResult[][],
): IntermediateFlowResult => {
  const nodes: Node[] = [];
  const edges: Edge[] = [];
  let nextLinkFns: ((id: string) => Edge)[] = [];

  for (const group of groupings) {
    nodes.push(...group.flatMap((g) => g.nodes));
    edges.push(
      ...group.flatMap((g) => g.edges),
      ...nextLinkFns.map((fn) => fn(group[0].startId)),
      ...pairwise(
        (prev, current) =>
          createEdge(prev.startId, current.startId, "attempted"),
        group,
      ),
    );
    nextLinkFns = group.flatMap((g) => g.nextLinkFns);
  }

  return {
    startId: groupings[0][0].startId,
    nodes,
    edges,
    nextLinkFns,
  };
};

const edgeTypes: ButtonEdges = {
  buttonEdge: ButtonEdge,
};

function renderGraph(executionList: ExpandableExecution[]): [Node[], Edge[]] {
  const executionListNoDisabled = executionList.filter(
    (e) => e.requirement !== "DISABLED",
  );
  const groupings = [
    [borderStep(createNode({ id: "start", displayName: "Start" }, "input"))],
    ...createConcurrentGroupings(executionListNoDisabled),
    [
      borderStep(
        createNode({ id: "end", displayName: "End" }, "output"),
        false,
      ),
    ],
  ];

  const { nodes, edges } = createGraph(groupings);

  return [getLayoutedNodes(nodes), getLayoutedEdges(edges)];
}

export const FlowDiagram = ({
  executionList: { expandableList },
}: FlowDiagramProps) => {
  const [expandDrawer, setExpandDrawer] = useState(false);
  const [initialNodes, initialEdges] = useMemo(
    () => renderGraph(expandableList),
    [],
  );
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  useUpdateEffect(() => {
    const [nodes, edges] = renderGraph(expandableList);
    setNodes(nodes);
    setEdges(edges);
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
