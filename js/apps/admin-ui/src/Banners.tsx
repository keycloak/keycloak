import { Banner, Flex, FlexItem } from "@patternfly/react-core";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";
import { ReactNode } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useRealm } from "./context/realm-context/RealmContext";
import { useWhoAmI } from "./context/whoami/WhoAmI";
import { toAddAdminUser } from "./user/routes/AddUser";

type WarnBannerProps = {
  msg: string | ReactNode;
};

const WarnBanner = ({ msg }: WarnBannerProps) => {
  const { t } = useTranslation();

  return (
    <Banner
      screenReaderText={typeof msg === "string" ? t(msg) : ""}
      variant="gold"
      isSticky
    >
      <Flex
        spaceItems={{ default: "spaceItemsSm" }}
        flexWrap={{ default: "wrap" }}
      >
        <FlexItem style={{ whiteSpace: "normal" }}>
          <ExclamationTriangleIcon style={{ marginRight: "0.3rem" }} />
          {typeof msg === "string" ? t(msg) : msg}
        </FlexItem>
      </Flex>
    </Banner>
  );
};

export const Banners = () => {
  const { whoAmI } = useWhoAmI();
  const { realm } = useRealm();

  if (whoAmI.isTemporary())
    return (
      <WarnBanner
        msg={
          <Trans i18nKey="loggedInAsTempAdminUser">
            You are logged in as a temporary admin user. To harden security,
            <Link to={toAddAdminUser({ realm })}>
              create a permanent admin account
            </Link>
            and delete the temporary one.
          </Trans>
        }
      />
    );
};
