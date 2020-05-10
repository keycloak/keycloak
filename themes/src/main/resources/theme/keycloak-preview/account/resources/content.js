
var content = [
    {
        id: 'personal-info',
        path: 'personal-info',
        label: 'personalInfoHtmlTitle',
        modulePath: '/app/content/account-page/AccountPage',
        componentName: 'AccountPage'
    },
    {
        id: 'security',
        label: 'Account Security',
        content: [
            {
                id: 'signingin',
                path: 'security/signingin',
                label: 'signingIn',
                modulePath: '/app/content/signingin-page/SigningInPage',
                componentName: 'SigningInPage',
            },
          /*  {
                path: 'security/password',
                label: 'password',
                modulePath: '/app/content/aia-page/AppInitiatedActionPage',
                componentName: 'AppInitiatedActionPage',
                kcAction: 'UPDATE_PASSWORD'
            },
            {
                path: 'security/authenticator',
                label: 'authenticator',
                modulePath: '/app/content/aia-page/AppInitiatedActionPage',
                componentName: 'AppInitiatedActionPage',
                kcAction: 'CONFIGURE_TOTP'
            }, */
            {
                id: 'device-activity',
                path: 'security/device-activity',
                label: 'device-activity',
                modulePath: '/app/content/device-activity-page/DeviceActivityPage',
                componentName: 'DeviceActivityPage'
            },
            {
                id: 'linked-accounts',
                path: 'security/linked-accounts',
                label: 'linkedAccountsHtmlTitle',
                modulePath: '/app/content/linked-accounts-page/LinkedAccountsPage',
                componentName: 'LinkedAccountsPage',
                hidden: !features.isLinkedAccountsEnabled
            }
        ]
    },
    {
        id: 'applications',
        path: 'applications',
        label: 'applications',
        modulePath: '/app/content/applications-page/ApplicationsPage',
        componentName: 'ApplicationsPage'
    },
    {
        id: 'resources',
        path: 'resources',
        label: 'resources',
        modulePath: '/app/content/my-resources-page/MyResourcesPage',
        componentName: 'MyResourcesPage',
        hidden: !features.isMyResourcesEnabled
    }
];
