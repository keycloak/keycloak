import { Icon } from "@patternfly/react-core";
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

import TideIcon  from "../../../../assets/icons/tide-icon.svg";

type IconMapperProps = {
  icon: string;
};

export const IconMapper = ({ icon }: IconMapperProps) => {
  const SpecificIcon = getIcon(icon);
  return (
    <Icon size="lg">
      <SpecificIcon alt={icon} />
    </Icon>
  );
};

function getIcon(icon: string) {
  switch (icon) {
    case "github":
      return GithubIcon;
    case "facebook":
      return FacebookSquareIcon;
    case "gitlab":
      return GitlabIcon;
    case "google":
      return GoogleIcon;
    case "linkedin":
    case "linkedin-openid-connect":
      return LinkedinIcon;

    case "openshift-v4":
      return OpenshiftIcon;
    case "stackoverflow":
      return StackOverflowIcon;
    case "twitter":
      return TwitterIcon;
    case "microsoft":
      return MicrosoftIcon;
    case "bitbucket":
      return BitbucketIcon;
    case "instagram":
      return InstagramIcon;
    case "paypal":
      return PaypalIcon;
    case "tide":
      return () => <img src={TideIcon} alt="Tide Icon" width="16" height="16" />; // TIDE IMPLEMENTATION
    default:
      return CubeIcon;
  }
}
