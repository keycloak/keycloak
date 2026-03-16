import type { KeycloakAdminClient } from "../client.js";
import { RequiredActionAlias } from "../defs/requiredActionProviderRepresentation.js";
import Resource from "./resource.js";

/* TIDECLOAK IMPLEMENTATION */
interface stripeCheckoutSessionResponse {
    message: string,
    activationPackage: string,
    redirectUrl: string,

}

/* TIDECLOAK IMPLEMENTATION */
interface License {
    licenseData: string;
    status: string;
    date: string;
}

/* TIDECLOAK IMPLEMENTATION */
interface licenseDetails {
    currentUserAcc: string,
    expiryDate: number,
}

/* TIDECLOAK IMPLEMENTATION */
interface scheduledTaskInfo {
    taskName: string,
    startDateMillis: number,
    delayMillis: number,

}

/* TIDECLOAK IMPLEMENTATION */
export class TideProvider extends Resource<{ realm?: string }> {
    /* # TIDECLOAK IMPLEMENTATION */
    public getRequiredActionLink = this.makeRequest<
        {
            userId: string;
            clientId?: string;
            lifespan?: number;
            redirectUri?: string;
            actions?: (RequiredActionAlias | string)[];
        },
        string
    >({
        method: "POST",
        path: "/tideAdminResources/get-required-action-link",
        payloadKey: "actions",
        queryParamKeys: ["lifespan", "redirectUri", "clientId", "userId"],
        keyTransform: {
            clientId: "client_id",
            redirectUri: "redirect_uri",
        },
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public toggleRagnarok = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/ragnarok/toggle-ragnarok",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public addAuthorization = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/add-authorization",
    });
    /* # TIDECLOAK IMPLEMENTATION */
    public addRejection = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/add-rejection",
    });
    /* # TIDECLOAK IMPLEMENTATION */
    public addReview = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/add-review",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public saveFirstAdminAuthorizer = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/vendorResources/first-admin-authorizer"
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getVouchers = this.makeRequest<FormData, string>({
        method: "POST",
        path: "/tideAdminResources/new-voucher"
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public rotateVrk = this.makeRequest<void, string>({
        method: "POST",
        path: "/vendorResources/rotate-vrk"
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public switchVrk = this.makeRequest<{ gvrk?: string }, Response>({
    method: "POST",
    path: "/vendorResources/switch-vrk",
    queryParamKeys: ["gvrk"],
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getScheduledTasks = this.makeRequest<void, scheduledTaskInfo[]>({
        method: "GET",
        path: "/vendorResources/scheduledTasks",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getLicenseHistory = this.makeRequest<void, License[]>({
        method: "GET",
        path: "/vendorResources/licenseHistory",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public triggerScheduledTask = this.makeRequest<{ taskName: string }, Response>({
        method: "POST",
        path: "/vendorResources/scheduledTasks/{taskName}/trigger",
        urlParamKeys: ["taskName"],
        catchNotFound: true,
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public scheduleGenVRKTask = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/scheduledTasks/genVRK/schedule",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public uploadImage = this.makeRequest<FormData, Record<string, string>>({
        method: "POST",
        path: "/tide-idp-admin-resources/images/upload",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getImageName = this.makeRequest<{ type: string }, string | null>({
        method: "GET",
        path: "/tide-idp-admin-resources/images/{type}/name",
        urlParamKeys: ["type"],
        catchNotFound: true,
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public deleteImage = this.makeRequest<{ type: string }, Response>({
        method: "DELETE",
        path: "/tide-idp-admin-resources/images/{type}/delete",
        urlParamKeys: ["type"],
        catchNotFound: true,
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public generateInitialKey = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/generate-initial-key",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public reAddTideKey = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/readd-tide-key",
    });
    /* # TIDECLOAK IMPLEMENTATION */
    public signIdpSettings = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/sign-idp-settings",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public generateInitialVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/generate-initial-vrk",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public confirmInitialVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/confirm-initial-vrk",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public clearTempVrk = this.makeRequest<void, Response>({
        method: "POST",
        path: "/vendorResources/clear-temp-vrk",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public generateVendorId = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/generate-vendor-id",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public signMessage = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/sign-message",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public authorizeStripeRequest = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/authorize-stripe-request",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public createStripeCheckoutSession = this.makeRequest<FormData, stripeCheckoutSessionResponse>({
        method: "POST",
        path: "/vendorResources/createStripeCheckoutSession",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public isPendingLicenseActive = this.makeRequest<void, boolean>({
        method: "GET",
        path: "/vendorResources/isPendingLicenseActive",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getLicenseDetails = this.makeRequest<void, licenseDetails>({
        method: "GET",
        path: "/vendorResources/getLicenseDetails",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getSubscriptionStatus = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/getSubscriptionStatus",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public createCustomerPortalSession = this.makeRequest<FormData, stripeCheckoutSessionResponse>({
        method: "POST",
        path: "/vendorResources/createCustomerPortalSession",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public updateSubscription = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/vendorResources/updateSubscription",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public cancelSubscription = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/cancelSubscription",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getInstallationProviders = this.makeRequest<
        { clientId: string; providerId: string },
        string
    >({
        method: "GET",
        path: "/vendorResources/get-installations-provider",
        queryParamKeys: ["clientId", "providerId"],
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public getTideJwk = this.makeRequest<void, Response>({
        method: "GET",
        path: "/vendorResources/get-tide-jwk",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public toggleIGA = this.makeRequest<FormData, Response>({
        method: "POST",
        path: "/tide-admin/toggle-iga",
    });

    /* # TIDECLOAK IMPLEMENTATION */
    public triggerLicenseRenewedEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerLicenseRenewedEvent/{error}"
    })
    /* # TIDECLOAK IMPLEMENTATION */
    public triggerVendorKeyCreationEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerVendorKeyCreationEvent/{error}"
    })
    /* # TIDECLOAK IMPLEMENTATION */
    public triggerAuthorizerUpdateEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerAuthorizerUpdateEvent/{error}"
    })
    /* # TIDECLOAK IMPLEMENTATION */
    public triggerAuthorizeEvent = this.makeRequest<{ error: boolean }, void>({
        method: "GET",
        urlParamKeys: ["error"],
        path: "/vendorResources/triggerAuthorizeEvent/{error}"
    })

    /* # TIDECLOAK IMPLEMENTATION */
    public offboardProvider = this.makeRequest<void, string>({
        method: "POST",
        path: "/ragnarok/trigger-offboarding",
    });
    /* # TIDECLOAK IMPLEMENTATION */
    public licenseProvider = this.makeRequest<{ gvrk?: string }, string>({
        method: "POST",
        path: "/tideAdminResources/trigger-license-signing",
        queryParamKeys: ["gvrk"],
    });

    constructor(client: KeycloakAdminClient) {
        super(client, {
            path: "/admin/realms/{realm}",
            getUrlParams: () => ({
                realm: client.realmName,
            }),
            getBaseUrl: () => client.baseUrl,
        });
    }
}
