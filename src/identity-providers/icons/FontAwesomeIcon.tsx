import React from "react";
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
          src={environment.resourceUrl + "./bitbucket-brands.svg"}
          {...styles}
          aria-label="bitbucket icon"
        />
      );
    case "microsoft":
      return (
        <img
          src={environment.resourceUrl + "./microsoft-brands.svg"}
          {...styles}
          aria-label="microsoft icon"
        />
      );
    case "instagram":
      return (
        <img
          src={environment.resourceUrl + "./instagram-brands.svg"}
          {...styles}
          aria-label="instagram icon"
        />
      );
    case "paypal":
      return (
        <img
          src={environment.resourceUrl + "./paypal-brands.svg"}
          {...styles}
          aria-label="paypal icon"
        />
      );
    default:
      return null;
  }
};
