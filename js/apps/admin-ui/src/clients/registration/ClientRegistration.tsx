import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toClientRegistration } from "../routes/ClientRegistration";
import { ClientRegistrationList } from "./ClientRegistrationList";

export const ClientRegistration = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const anonymousTab = useRoutableTab(
    toClientRegistration({ realm, subTab: "anonymous" }),
  );
  const authenticatedTab = useRoutableTab(
    toClientRegistration({ realm, subTab: "authenticated" }),
  );

  return (
    <RoutableTabs
      defaultLocation={toClientRegistration({ realm, subTab: "anonymous" })}
      mountOnEnter
      unmountOnExit
    >
      <Tab
        data-testid="anonymous"
        title={
          <TabTitleText>
            {t("anonymousAccessPolicies")}{" "}
            <HelpItem
              fieldLabelId=""
              helpText={t("anonymousAccessPoliciesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        {...anonymousTab}
      >
        <PageSection>
          <ClientRegistrationList subType="anonymous" />
        </PageSection>
      </Tab>
      <Tab
        data-testid="authenticated"
        title={
          <TabTitleText>
            {t("authenticatedAccessPolicies")}{" "}
            <HelpItem
              fieldLabelId=""
              helpText={t("authenticatedAccessPoliciesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        {...authenticatedTab}
      >
        <PageSection>
          <ClientRegistrationList subType="authenticated" />
        </PageSection>
      </Tab>
    </RoutableTabs>
  );
};
