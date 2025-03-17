import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { Label } from "@patternfly/react-core";
import {
  CodeBranchIcon,
  MapMarkerIcon,
  ProcessAutomationIcon,
  TaskIcon,
} from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useAuthenticationProvider } from "./AuthenticationProviderContext";
import { FlowType } from "./FlowRow";

type FlowTitleProps = {
  id?: string;
  type: FlowType;
  title: string;
  subtitle: string;
  providerId?: string;
};

const FlowIcon = ({ type }: { type: FlowType }) => {
  switch (type) {
    case "condition":
      return <TaskIcon />;
    case "flow":
      return <CodeBranchIcon />;
    case "execution":
      return <ProcessAutomationIcon />;
    case "step":
      return <MapMarkerIcon />;
    default:
      return undefined;
  }
};

function mapTypeToColor(type: FlowType) {
  switch (type) {
    case "condition":
      return "purple";
    case "flow":
      return "green";
    case "execution":
      return "blue";
    case "step":
      return "cyan";
    default:
      return "grey";
  }
}

export const FlowTitle = ({
  id,
  type,
  title,
  subtitle,
  providerId,
}: FlowTitleProps) => {
  const { t } = useTranslation();
  const { providers } = useAuthenticationProvider();
  const helpText =
    providers?.find((p) => p.id === providerId)?.description || subtitle;
  return (
    <div data-testid={title}>
      <span data-id={id} id={`title-id-${id}`}>
        <Label icon={<FlowIcon type={type} />} color={mapTypeToColor(type)}>
          {t(type)}
        </Label>{" "}
        {title}{" "}
        {helpText && <HelpItem helpText={helpText} fieldLabelId={id!} />}
      </span>
    </div>
  );
};
