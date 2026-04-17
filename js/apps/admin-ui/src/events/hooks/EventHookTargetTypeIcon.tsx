import { CubeIcon, DatabaseIcon, DownloadIcon, GlobeIcon } from "@patternfly/react-icons";

type EventHookTargetTypeIconProps = {
    type?: string;
};

export const EventHookTargetTypeIcon = ({ type }: EventHookTargetTypeIconProps) => {
    switch (type) {
        case "http":
            return <GlobeIcon />;
        case "pull":
            return <DownloadIcon />;
        case "sql":
            return <DatabaseIcon />;
        default:
            return <CubeIcon />;
    }
};
