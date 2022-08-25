import {
  CubeIcon,
  FacebookSquareIcon,
  GithubIcon,
  GitlabIcon,
  GoogleIcon,
  LinkedinIcon,
  OpenshiftIcon,
  StackOverflowIcon,
  TwitterIcon,
} from "@patternfly/react-icons";
import type { SVGIconProps } from "@patternfly/react-icons/dist/js/createIcon";

import { FontAwesomeIcon } from "./icons/FontAwesomeIcon";

type ProviderIconMapperProps = {
  provider: { [index: string]: string };
};

export const ProviderIconMapper = ({ provider }: ProviderIconMapperProps) => {
  const defaultProps: SVGIconProps = { size: "lg" };
  switch (provider.id) {
    case "github":
      return <GithubIcon {...defaultProps} />;
    case "facebook":
      return <FacebookSquareIcon {...defaultProps} />;
    case "gitlab":
      return <GitlabIcon {...defaultProps} />;
    case "google":
      return <GoogleIcon {...defaultProps} />;
    case "linkedin":
      return <LinkedinIcon {...defaultProps} />;

    case "openshift-v3":
    case "openshift-v4":
      return <OpenshiftIcon {...defaultProps} />;
    case "stackoverflow":
      return <StackOverflowIcon {...defaultProps} />;
    case "twitter":
      return <TwitterIcon {...defaultProps} />;
    case "microsoft":
    case "bitbucket":
    case "instagram":
    case "paypal":
      return <FontAwesomeIcon icon={provider.id} />;
    default:
      return <CubeIcon {...defaultProps} />;
  }
};
