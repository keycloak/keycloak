import {
  useWatch,
  useForm
} from "react-hook-form";
import {
  AlertVariant,
  FormGroup,
  ClipboardCopy,
  Label,
  Button,
  Text,
  Spinner
} from "@patternfly/react-core";
import { HelpItem, ScrollForm } from "@keycloak/keycloak-ui-shared";
import { useState, FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../form/FormAccess.js";
import { KEY_PROVIDER_TYPE } from "../../util.js";
import { useRealm } from "../../context/realm-context/RealmContext.js";
import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAdminClient } from "../../admin-client.js";
import { useParams } from "../../utils/useParams.js";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { License, TideLicenseHistory } from "./TideLicenseHistory";
import { ScheduledTaskInfo, TideScheduledTasks } from "./TideScheduledTasks.js";

// TIDECLOAK IMPLEMENTATION
type TideLicensingTabProps = {
  refreshCallback?: () => Promise<void> | undefined;
};

enum LicensingTiers {
  Free = 'FreeTier',
};

export const TideLicensingTab: FC<TideLicensingTabProps> = ({ refreshCallback }) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();

  const [scheduledTasks, setScheduledTasks] = useState<ScheduledTaskInfo[]>([]);
  const [activeLicenseDetails, setActiveLicenseDetails] = useState<string>("");
  const [licensingHistory, setLicensingHistory] = useState<License[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isPendingResign, setIsPendingResign] = useState<boolean>(false);
  const [isInitialCheckout, setIsInitialCheckout] = useState<boolean>(false);


  const [key, setKey] = useState(0);
  const { realm, realmRepresentation } = useRealm();
  const { addAlert } = useAlerts();
  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });
  const { getValues, reset, control, setValue } = form;
  const [currentUsers, setCurrentUsers] = useState<string>("0");
  const [licenseExpiry, setLicenseExpiry] = useState<string>("0");
  const [licenseMaxUserAcc, setLicenseMaxUserAcc] = useState<string>("0");
  const { id } = useParams<{ id: string }>();

  const fieldNames = [
    "config.gVRK",
    "config.payerPublic",
    "config.vendorId",
    "config.pendingGVRK",
    "config.pendingVendorId",
    "config.vvkId",
    "config.customerId",
    "config.maxUserAcc",
    "config.initialSessionId",
    "config.systemHomeOrk"
  ] as const;

  // Function to ensure each watched field is a single string
  function getSingleValue(value: string | string[]): string {
    return Array.isArray(value) ? value[0] : value;
  }

  // Use `useWatch` for each field and apply type narrowing
  const watchedConfig = fieldNames.reduce((acc, fieldName) => {
    acc[fieldName] = getSingleValue(useWatch({ control, name: fieldName }));
    return acc;
  }, {} as Record<typeof fieldNames[number], string>);
  // Now you can access each config like so:
  const {
    ["config.gVRK"]: watchConfigGVRK,
    ["config.payerPublic"]: watchConfigPayerPub,
    ["config.vendorId"]: watchConfigVendorId,
    ["config.pendingGVRK"]: watchConfigPendingGVRK,
    ["config.pendingVendorId"]: watchConfigPendingVendorId,
    ["config.vvkId"]: watchConfigVVKId,
    ["config.customerId"]: watchConfigCustomerId,
    ["config.maxUserAcc"]: watchConfigMaxUserAcc,
  } = watchedConfig;


  useFetch(
    async () => {
      if (id) return await adminClient.components.findOne({ id });
    },
    (result) => {
      if (result) {
        reset({ ...result });
      }
    },
    [],
  );

  // Helper functions
  const hasValue = (value: string) => value !== undefined && value !== null && value !== "" ? true : false;

  const retry = async (fn: () => Promise<boolean | undefined>, retries = 3, delay = 1000) => {
    for (let i = 0; i < retries; i++) {
      try {
        const result = await fn();
        if (result) {
          return result; // Success, return the result
        }
      } catch (error) {
        console.error(`Attempt ${i + 1} failed. Retrying...`, error);
      }
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, delay));
    }
    throw new Error(`Failed after ${retries} retries`);
  };

  const isLicensePending = () => {
    const hash = window.location.hash;
    const queryIndex = hash.indexOf('?');

    if (queryIndex !== -1) {
      const queryString = hash.substring(queryIndex + 1); // Remove the part before '?'
      const queryParams = new URLSearchParams(queryString);

      const retryLicenseActivation = queryParams.get("licensePending") === "true";
      // Remove the query parameters from the hash, no longer need it
      window.location.hash = hash.substring(0, queryIndex); // Keep only the part before the '?'
      return retryLicenseActivation;
    }
    // Return false if no query parameters are found
    return false;
  };


  useEffect(() => {
    const activateLicense = async () => {
      try {
        let signSettingsRequired;
        if (!hasValue(watchConfigVVKId) && isLicensePending()) {
          // Retry every second for a minute
          signSettingsRequired = await retry(async () => await checkLicenseActive(), 60);
        } else {
          var isLicenseActive = await checkLicenseActive()
          signSettingsRequired = isLicenseActive;
        }
        // license renewed
        if (signSettingsRequired) await adminClient.tideAdmin.triggerLicenseRenewedEvent({ error: false });
        if (signSettingsRequired) await adminClient.tideAdmin.triggerLicenseRenewedEvent({ error: false });

        if (signSettingsRequired) {
          await adminClient.tideAdmin.generateInitialKey();
          await refresh(); // refresh current page
          setIsLoading(false); // Loading is done
          setIsPendingResign(false);
        } else if (!isInitialCheckout) {
          setIsLoading(false);
        } else if (!isInitialCheckout) {
          setIsLoading(false);
        }
      } catch (err) {
        console.error(err);
        // license renewed error
        await adminClient.tideAdmin.triggerLicenseRenewedEvent({ error: true });
        setIsLoading(false);
        setIsInitialCheckout(false);
      }
    };

    if (!isPendingResign && hasValue(watchConfigPendingVendorId)) {
      setIsPendingResign(true);
      setIsLoading(true);
      activateLicense();
    }
  }, [watchConfigPendingVendorId]);

  useEffect(() => {
    const licenseDetails = JSON.stringify(
      {
        vvkId: watchConfigVVKId,
        customerId: watchConfigCustomerId,
        gVRK: watchConfigGVRK,
        vendorId: watchConfigVendorId,
        payerPub: watchConfigPayerPub
      },
      null,
      2
    );
    setActiveLicenseDetails(licenseDetails);
  }, [watchConfigGVRK, watchConfigPayerPub, watchConfigVendorId, watchConfigVVKId]);

  useEffect(() => {
    const fetchLicenseDetails = async () => {
      if (hasValue(activeLicenseDetails)) {
        const response = await adminClient.tideAdmin.getLicenseDetails();
        const date = new Date(response.expiryDate * 1000);
        const day = date.getUTCDate().toString().padStart(2, '0');
        const month = (date.getUTCMonth() + 1).toString().padStart(2, '0'); // Months are zero-based
        const year = date.getUTCFullYear().toString().slice(-2);
        const formattedDate = `${day}/${month}/${year}`;

        setCurrentUsers(response.currentUserAcc);
        setLicenseMaxUserAcc(watchConfigMaxUserAcc);
        setLicenseExpiry(formattedDate);
      }
    };
    if (hasValue(watchConfigVVKId) !== undefined && hasValue(watchConfigVendorId)) {
      fetchLicenseDetails();
    }
  }, [watchConfigVVKId, watchConfigMaxUserAcc, key, watchConfigVendorId, activeLicenseDetails]);

  const checkLicenseActive = async () => {
    try {
      const provider = await adminClient.components.findOne({ id });
      const isActive = await adminClient.tideAdmin.isPendingLicenseActive();
      const isInitialSetup = provider?.config?.vvkId !== undefined ? !hasValue(getSingleValue(provider?.config?.vvkId)) : true;

      return isActive && isInitialSetup;
    } catch (error) {
      console.error('Error checking license:', error);
      return false; // Return false in case of an error
    }
  };

  const refresh = async () => {
    const latest = await adminClient.components.findOne({ id });
    reset(latest);
    setKey(key + 1);
  };

  const save = async (savedProvider?: ComponentRepresentation) => {
    const updatedProvider = await adminClient.components.findOne({ id });
    if (!updatedProvider) {
      throw new Error(t("notFound"));
    }

    const p = savedProvider || getValues();
    const config: ComponentRepresentation = { ...updatedProvider, ...p }
    try {
      await adminClient.components.update(
        { id },
        {
          ...config,
          providerType: KEY_PROVIDER_TYPE,
        },
      );
      addAlert(t("saveProviderSuccess"), AlertVariant.success);
      addAlert(t("newLicenseActivatedIdentityProvider"), AlertVariant.success);
    } catch (error) {
      addAlert(t("newLicenseErrorIdentityProvider"), AlertVariant.danger);
    }
  };

  const handleCheckout = async (licensingTier: string) => {
    try {
      setIsInitialCheckout(true);
      setIsLoading(true);
      const redirectUrl = window.location.href.endsWith('/') ? window.location.href.slice(0, -1) : window.location.href;

      const data = new FormData();
      data.append("redirectUrl", redirectUrl);
      data.append("licensingTier", licensingTier);

      const response = await adminClient.tideAdmin.createStripeCheckoutSession(data);
      window.location.href = response.redirectUrl;

    } catch (err) {
      setIsLoading(false);
      setIsInitialCheckout(false);

      addAlert(t("Error with checkout"), AlertVariant.danger);
      throw err;
    }
  };

  const saveActivationPackage = async (activationPackage: string) => {
    const activationPackageJson = JSON.parse(activationPackage);
    validateActivationPackage(activationPackageJson);

    const sessionId = activationPackageJson.sessionId;
    const customerId = activationPackageJson.customerId;
    const payerPub = activationPackageJson.payerPublic;
    const maxUserAcc = activationPackageJson.maxUserAcc;

    setValue("config.initialSessionId", [sessionId]);
    setValue("config.customerId", [customerId]);
    setValue("config.payerPublic", [payerPub]);
    setValue("config.maxUserAcc", [maxUserAcc]);
  };

  const generateInitialVrk = async () => {
    await adminClient.tideAdmin.generateInitialVrk();
    const updatedProvider = await adminClient.components.findOne({ id });
    reset(updatedProvider);
    const pendingGVRK = updatedProvider?.config?.pendingGVRK !== undefined ? getSingleValue(updatedProvider?.config?.pendingGVRK) : undefined;
    const pendingVendorId = updatedProvider?.config?.pendingVendorId !== undefined ? getSingleValue(updatedProvider?.config?.pendingVendorId) : undefined

    if (pendingGVRK !== undefined && hasValue(pendingGVRK) && pendingVendorId !== undefined && hasValue(pendingVendorId)) {
      return { GVRK: pendingGVRK, VendorId: pendingVendorId };
    } else {
      return null;
    }
  };

  const validateActivationPackage = (activationPackageJson: any) => {
    if (
      !activationPackageJson.gVRK ||
      !activationPackageJson.payerPublic ||
      !activationPackageJson.licenseId ||
      !activationPackageJson.maxUserAcc ||
      !activationPackageJson.sessionId ||
      !activationPackageJson.customerId
    ) {
      throw new Error("Invalid activation package provided");
    }
    // Check if these values match the temp license request
    const gVRK = activationPackageJson.gVRK;
    const vendorId = activationPackageJson.licenseId;
    const pendingGVRK = getSingleValue(getValues("config.pendingGVRK"));
    const pendingVendorId = getSingleValue(getValues("config.pendingVendorId"));

    if ((hasValue(pendingGVRK) && gVRK !== pendingGVRK) || (hasValue(pendingVendorId) && vendorId !== pendingVendorId)) {
      throw new Error("Incorrect activation package provided, this is for the wrong license request");
    }

    return true;
  };

  const generateJWK = async () => {
    var content = await adminClient.tideAdmin.getTideJwk();
    var jwk = JSON.stringify(content)
    const blob = new Blob([jwk], { type: 'text/plain' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'tide-eddsa.jwk';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  }

  const handleManageSubscription = async () => {
    const redirectUrl = window.location.href.endsWith('/') ? window.location.href.slice(0, -1) : window.location.href;
    const form = new FormData();
    form.append("redirectUrl", redirectUrl)
    const response = await adminClient.tideAdmin.createCustomerPortalSession(form);
    window.location.href = response.redirectUrl;
  };

  const getScheduledTasks = async () => {
    try {
      const response = await adminClient.tideAdmin.getScheduledTasks();
      // Filter tasks based on criteria
      const filteredTasks = response.filter(
        task =>
          task.taskName.startsWith("tide") && // Starts with 'tide'
          task.taskName.endsWith(realmRepresentation!.id!) // Matches current realm
      );
      setScheduledTasks(filteredTasks); // Update state with filtered tasks
    } catch (error) {
      console.error("Failed to fetch scheduled tasks:", error);
    }
  };

  const fetchPendingLicense = async () => {
    if (watchConfigPendingGVRK === undefined || watchConfigPendingGVRK === "") {
      return null
    }
    const tempLicenseDetails = {
      vvkId: watchConfigVVKId,
      customerId: watchConfigCustomerId,
      gVRK: watchConfigPendingGVRK,
      vendorId: watchConfigPendingVendorId,
      payerPub: watchConfigPayerPub
    };
    const utcNowTimestamp = Date.now();
    const authForm = new FormData();
    authForm.append("data", utcNowTimestamp.toString());
    const response = await adminClient.tideAdmin.getSubscriptionStatus();
    const pendingLicense = { licenseData: JSON.stringify(tempLicenseDetails, null, 2), status: await response.toString() ?? "", date: licenseExpiry }


    return pendingLicense;
  };

  const getLicenseHistory = async () => {
    try {

      const response: License[] = await adminClient.tideAdmin.getLicenseHistory();
      const pendingLicense = await fetchPendingLicense();
      if (pendingLicense !== null) {
        response.unshift(pendingLicense);
      }

      setLicensingHistory(response); // Update state with filtered tasks
    } catch (error) {
      console.error("Failed to fetch license history:", error);
    }
  };

  useEffect(() => {
    getScheduledTasks();
  }, [realm, key]);

  useEffect(() => {
    getLicenseHistory()
  }, [watchConfigPendingVendorId, watchConfigPayerPub, watchConfigPendingGVRK, watchConfigVVKId, watchConfigVendorId, key]);


  const sections = [
    {
      title: t("Active License"),
      panel: (
        <FormAccess role="manage-identity-providers" isHorizontal>
          {isLoading ? (
            <Spinner size="xl" />
          ) : hasValue(watchConfigVVKId) ? (
            <>
              {/* Existing form groups for active license */}
              <FormGroup
                label={t("License Details")}
                labelIcon={
                  <HelpItem
                    helpText={"This is the details of your current active license. Save a copy locally."}
                    fieldLabelId={"LicenseDetails"}
                  />
                }
                fieldId="active-license-details"
              >
                <ClipboardCopy isCode isReadOnly>{activeLicenseDetails}</ClipboardCopy>
              </FormGroup>

              <FormGroup
                label={t("Expiry Date")}
                labelIcon={
                  <HelpItem
                    helpText={"The expiry date of this active license"}
                    fieldLabelId={"LicenseExpiry"}
                  />
                }
                fieldId="license-expiry"
              >
                <Label>{licenseExpiry}</Label>
              </FormGroup>

              <FormGroup
                label={t("Max User Accounts")}
                labelIcon={
                  <HelpItem
                    helpText={"The max amount of user accounts for this license"}
                    fieldLabelId={"LicenseMaxUserAccounts"}
                  />
                }
                fieldId="license-max-user-accounts"
              >
                <Label>{licenseMaxUserAcc}</Label>
              </FormGroup>

              <FormGroup
                label={t("Current User Accounts")}
                labelIcon={
                  <HelpItem
                    helpText={"The current amount of user accounts on this license"}
                    fieldLabelId={"LicenseCurrentUserAccounts"}
                  />
                }
                fieldId="license-current-user-accounts"
              >
                <Label>{currentUsers}</Label>
              </FormGroup>
              <FormGroup
                label={t("JWK")}
                labelIcon={
                  <HelpItem
                    helpText={"JWK needed for client authentication"}
                    fieldLabelId={"LicenseJWK"}
                  />
                }
                fieldId="license-jwk"
              >
                <Button type="button" onClick={async () => await generateJWK()}>Export</Button>
              </FormGroup>
              <FormGroup
                label={t("License Subscription")}
                labelIcon={
                  <HelpItem
                    helpText={"Manage your subscription here."}
                    fieldLabelId={"LicenseSubscription"}
                  />
                }
                fieldId="license-subscription"
              >
                <Button type="button" onClick={async () => await handleManageSubscription()}>Manage</Button>
              </FormGroup>
            </>
          ) : (
            <>
              {/* Show "No active license found" and "Request License" button */}
              <FormGroup
                fieldId="no-active-license"
              >
                <Text>{t("No active license found.")}</Text>
              </FormGroup>
              <FormGroup
                fieldId="request-license"
              >
                <Button variant="primary" onClick={async () => await handleCheckout(LicensingTiers.Free)}>
                  {t("Request License")}
                </Button>
              </FormGroup>
            </>
          )}
        </FormAccess>
      ),
    },
    {
      title: t("Activity Log"),
      panel: (
        <TideLicenseHistory licenseList={licensingHistory} />
      )
    },
    {
      title: t("Scheduled Tasks"),
      panel: (
        <TideScheduledTasks scheduledTasks={scheduledTasks} refresh={refresh} />
      )
    },
  ];

  return (
    <>
      <FormAccess role="manage-identity-providers" isHorizontal>
        <ScrollForm
          label={t("jumpToSection")}
          className="pf-v5-u-px-lg"
          sections={sections}
        />
      </FormAccess>
    </>
  );
};