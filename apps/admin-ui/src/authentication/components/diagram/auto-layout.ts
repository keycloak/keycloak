import { Elements, Position, isNode } from "react-flow-renderer";
import { graphlib, layout } from "dagre";

const dagreGraph = new graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeWidth = 130;
const nodeHeight = 28;

export const getLayoutedElements = (elements: Elements, direction = "LR") => {
  const isHorizontal = direction === "LR";
  dagreGraph.setGraph({ rankdir: direction });

  elements.forEach((element) => {
    if (isNode(element)) {
      dagreGraph.setNode(element.id, {
        width: nodeWidth,
        height: nodeHeight,
      });
    } else {
      dagreGraph.setEdge(element.source, element.target);
    }
  });

  layout(dagreGraph);

  return elements.map((element) => {
    if (isNode(element)) {
      const nodeWithPosition = dagreGraph.node(element.id);
      element.targetPosition = isHorizontal ? Position.Left : Position.Top;
      element.sourcePosition = isHorizontal ? Position.Right : Position.Bottom;

      element.position = {
        x: nodeWithPosition.x - nodeWidth / 2 + Math.random() / 1000,
        y: nodeWithPosition.y - nodeHeight / 2,
      };
    }

    return element;
  });
};
