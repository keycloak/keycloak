import { memo } from "react";
import { Handle, Position } from "reactflow";

type NodeProps = {
  data: { label: string };
  selected: boolean;
};

type SubFlowNodeProps = NodeProps & {
  prefix: string;
};

const SubFlowNodeInner = ({
  data: { label },
  prefix,
  selected,
}: SubFlowNodeProps) => {
  return (
    <>
      <Handle position={Position.Right} type="source" />
      <div
        className={`react-flow__node-default keycloak__authentication__subflow_node ${
          selected ? "selected" : ""
        }`}
      >
        <div>
          {prefix} {label}
        </div>
      </div>
      <Handle position={Position.Left} type="target" />
    </>
  );
};

export const SubFlowNode = memo(SubFlowNodeInner);

export const StartSubFlowNode = ({ ...props }: NodeProps) => (
  <SubFlowNode {...props} prefix="Start" />
);
export const EndSubFlowNode = ({ ...props }: NodeProps) => (
  <SubFlowNode {...props} prefix="End" />
);
