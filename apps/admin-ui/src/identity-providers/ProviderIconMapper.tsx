import {
  BitbucketIcon,
  CubeIcon,
  FacebookSquareIcon,
  GithubIcon,
  GitlabIcon,
  GoogleIcon,
  InstagramIcon,
  LinkedinIcon,
  MicrosoftIcon,
  OpenshiftIcon,
  PaypalIcon,
  StackOverflowIcon,
  TwitterIcon,
} from "@patternfly/react-icons";
import type { SVGIconProps } from "@patternfly/react-icons/dist/js/createIcon";

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
      return <MicrosoftIcon {...defaultProps} />;
    case "bitbucket":
      return <BitbucketIcon {...defaultProps} />;
    case "instagram":
      return <InstagramIcon {...defaultProps} />;
    case "paypal":
      return <PaypalIcon {...defaultProps} />;
    default:
      return <CubeIcon {...defaultProps} />;
  }
};
