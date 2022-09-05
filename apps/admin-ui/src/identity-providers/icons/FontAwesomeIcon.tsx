import environment from "../../environment";

type FontAwesomeIconProps = {
  icon: "bitbucket" | "microsoft" | "instagram" | "paypal";
};

export const FontAwesomeIcon = ({ icon }: FontAwesomeIconProps) => {
  const styles = { style: { height: "2em", width: "2em" } };
  switch (icon) {
    case "bitbucket":
      return (
        <img
          src={environment.resourceUrl + "/bitbucket-brands.svg"}
          {...styles}
        />
      );
    case "microsoft":
      return (
        <img
          src={environment.resourceUrl + "/microsoft-brands.svg"}
          {...styles}
        />
      );
    case "instagram":
      return (
        <img
          src={environment.resourceUrl + "/instagram-brands.svg"}
          {...styles}
        />
      );
    case "paypal":
      return (
        <img src={environment.resourceUrl + "/paypal-brands.svg"} {...styles} />
      );
    default:
      return null;
  }
};
