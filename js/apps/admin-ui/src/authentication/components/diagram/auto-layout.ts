import { graphlib, layout } from "@dagrejs/dagre";
import { Edge, Node, Position } from "reactflow";

const dagreGraph = new graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeWidth = 130;
const nodeHeight = 40;
const nodeAfterConditionalHeight = 130;

export const getLayoutedNodes = (nodes: Node[], direction = "LR"): Node[] => {
  const isHorizontal = direction === "LR";
  dagreGraph.setGraph({ rankdir: direction });

  nodes.forEach((element, index) => {
    const prevNode = index > 0 ? nodes[index - 1] : undefined;
    dagreGraph.setNode(element.id, {
      width: nodeWidth,
      height:
        prevNode?.type === "conditional"
          ? nodeAfterConditionalHeight
          : nodeHeight,
    });
  });

  layout(dagreGraph);

  return nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    node.targetPosition = isHorizontal ? Position.Left : Position.Top;
    node.sourcePosition = isHorizontal ? Position.Right : Position.Bottom;

    node.position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    };

    return node;
  });
};

export const getLayoutedEdges = (edges: Edge[], direction = "LR"): Edge[] => {
  dagreGraph.setGraph({ rankdir: direction });

  edges.forEach((element) => {
    dagreGraph.setEdge(element.source, element.target);
  });

  layout(dagreGraph);

  return edges;
};
