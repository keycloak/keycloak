import {
  PageSection,
  ActionGroup,
  Button,
  FormGroup,
} from "@patternfly/react-core";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { convertToFormValues } from "../util";
import { useEffect } from "react";
import { Controller, useFormContext, FormProvider } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { HelpItem, FormPanel, useAlerts } from "@keycloak/keycloak-ui-shared";
import { AlertVariant } from "@patternfly/react-core";

export const RealmSettingsOid4vciTab = ({
  realm,
  save,
}: {
  realm: any;
  save: (realm: any) => void;
}) => {
  const { t } = useTranslation();
  const { addAlert } = useAlerts();
  const form = useFormContext();
  const { formState, handleSubmit, setValue } = form;

  // Show a global error notification if validation fails
  const onError = () => {
    addAlert(t("formValidationError"), AlertVariant.danger);
  };

  // Hydrate form values from realm attributes
  useEffect(() => {
    if (realm.attributes) {
      // Set the nonce lifetime value if it exists in attributes
      if (realm.attributes["vc.c-nonce-lifetime-seconds"]) {
        setValue(
          "attributes.vc.c-nonce-lifetime-seconds",
          realm.attributes["vc.c-nonce-lifetime-seconds"],
        );
      }
      // Add any other vc attributes that need hydration here
    }
  }, [realm.attributes, setValue]);

  return (
    <PageSection variant="light">
      <FormPanel title={t("oid4vciAttributesSectionTitle")}>
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-u-mt-lg"
            onSubmit={handleSubmit(save, onError)}
          >
            <FormGroup
              label={t("oid4vciNonceLifetime")}
              fieldId="oid4vciNonceLifetime"
              labelIcon={
                <HelpItem
                  helpText={t("oid4vciNonceLifetimeHelp")}
                  fieldLabelId="oid4vciNonceLifetime"
                />
              }
            >
              <Controller
                name="attributes.vc.c-nonce-lifetime-seconds"
                control={form.control}
                rules={{ required: t("required"), min: 30 }}
                render={({ field }) => (
                  <TimeSelector
                    {...field}
                    id="oid4vciNonceLifetime"
                    min={30}
                    units={["second", "minute", "hour"]}
                    value={field.value}
                    onChange={field.onChange}
                    data-testid="oid4vci-nonce-lifetime-seconds"
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("preAuthorizedCodeLifespan")}
              fieldId="preAuthorizedCodeLifespan"
              labelIcon={
                <HelpItem
                  helpText={t("preAuthorizedCodeLifespanHelp")}
                  fieldLabelId="preAuthorizedCodeLifespan"
                />
              }
            >
              <Controller
                name="attributes.preAuthorizedCodeLifespanS"
                control={form.control}
                rules={{ required: t("required"), min: 30 }}
                render={({ field }) => (
                  <TimeSelector
                    {...field}
                    id="preAuthorizedCodeLifespan"
                    min={30}
                    units={["second", "minute", "hour"]}
                    value={field.value}
                    onChange={field.onChange}
                    data-testid="pre-authorized-code-lifespan-s"
                  />
                )}
              />
            </FormGroup>
            <ActionGroup>
              <Button
                variant="primary"
                type="submit"
                data-testid="oid4vci-tab-save"
                isDisabled={!formState.isDirty}
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                onClick={() => convertToFormValues(realm, setValue)}
              >
                {t("revert")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </FormPanel>
    </PageSection>
  );
};
