import {
  CubeIcon,
  DatabaseIcon,
  DownloadIcon,
  EnvelopeIcon,
  GlobeIcon,
} from "@patternfly/react-icons";

type EventHookTargetTypeIconProps = {
  type?: string;
};

export const EventHookTargetTypeIcon = ({
  type,
}: EventHookTargetTypeIconProps) => {
  switch (type) {
    case "http":
      return <GlobeIcon />;
    case "pull":
      return <DownloadIcon />;
    case "email":
      return <EnvelopeIcon />;
    case "sql":
      return <DatabaseIcon />;
    default:
      return <CubeIcon />;
  }
};
