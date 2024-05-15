import { FormSubmitButton } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form/FormAccess";
import { AttributesForm } from "../components/key-value-form/AttributeForm";
import { arrayToKeyValue } from "../components/key-value-form/key-value-convert";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useFetch } from "../utils/useFetch";
import { useParams } from "../utils/useParams";
import { DetailOrganizationHeader } from "./DetailOraganzationHeader";
import { Members } from "./Members";
import {
  OrganizationForm,
  OrganizationFormType,
  convertToOrg,
} from "./OrganizationForm";
import {
  EditOrganizationParams,
  OrganizationTab,
  toEditOrganization,
} from "./routes/EditOrganization";

export default function DetailOrganization() {
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { realm } = useRealm();
  const { id } = useParams<EditOrganizationParams>();
  const { t } = useTranslation();

  const form = useForm<OrganizationFormType>();

  const save = async (org: OrganizationFormType) => {
    try {
      const organization = convertToOrg(org);
      await adminClient.organizations.updateById({ id }, organization);
      addAlert("organizationSaveSuccess");
    } catch (error) {
      addError("organizationSaveError", error);
    }
  };

  useFetch(
    () => adminClient.organizations.findOne({ id }),
    (org) => {
      if (!org) {
        throw new Error(t("notFound"));
      }
      form.reset({
        ...org,
        domains: org.domains?.map((d) => d.name),
        attributes: arrayToKeyValue(org.attributes),
      });
    },
    [id],
  );

  const useTab = (tab: OrganizationTab) =>
    useRoutableTab(
      toEditOrganization({
        realm,
        id,
        tab,
      }),
    );

  const settingsTab = useTab("settings");
  const attributesTab = useTab("attributes");
  const membersTab = useTab("members");

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <FormProvider {...form}>
        <DetailOrganizationHeader save={() => save(form.getValues())} />
        <RoutableTabs
          data-testid="organization-tabs"
          aria-label={t("organization")}
          isBox
          mountOnEnter
        >
          <Tab
            id="settings"
            data-testid="settingsTab"
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection>
              <FormAccess
                role="anyone"
                onSubmit={form.handleSubmit(save)}
                isHorizontal
              >
                <OrganizationForm />
                <ActionGroup>
                  <FormSubmitButton
                    formState={form.formState}
                    data-testid="save"
                  >
                    {t("save")}
                  </FormSubmitButton>
                  <Button data-testid="cancel" variant="link">
                    {t("cancel")}
                  </Button>
                </ActionGroup>
              </FormAccess>
            </PageSection>
          </Tab>
          <Tab
            id="attributes"
            data-testid="attributeTab"
            title={<TabTitleText>{t("attributes")}</TabTitleText>}
            {...attributesTab}
          >
            <PageSection variant="light">
              <AttributesForm
                form={form}
                save={save}
                reset={() =>
                  form.reset({
                    ...form.getValues(),
                  })
                }
                name="attributes"
              />
            </PageSection>
          </Tab>
          <Tab
            id="members"
            data-testid="membersTab"
            title={<TabTitleText>{t("members")}</TabTitleText>}
            {...membersTab}
          >
            <PageSection variant="light">
              <Members />
            </PageSection>
          </Tab>
        </RoutableTabs>
      </FormProvider>
    </PageSection>
  );
}
