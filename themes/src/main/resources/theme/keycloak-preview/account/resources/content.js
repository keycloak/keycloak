
var content = [
    {
        path: 'personal-info',
        label: 'personalInfoHtmlTitle',
        modulePath: '/app/content/account-page/AccountPage',
        componentName: 'AccountPage'
    },
    {
        label: 'Account Security',
        content: [
            {
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
                path: 'security/device-activity',
                label: 'device-activity',
                modulePath: '/app/content/device-activity-page/DeviceActivityPage',
                componentName: 'DeviceActivityPage'
            },
            {
                path: 'security/linked-accounts',
                label: 'linkedAccountsHtmlTitle',
                modulePath: '/app/content/linked-accounts-page/LinkedAccountsPage',
                componentName: 'LinkedAccountsPage',
                hidden: !features.isLinkedAccountsEnabled
            }
        ]
    },
    {
        path: 'applications',
        label: 'applications',
        modulePath: '/app/content/applications-page/ApplicationsPage',
        componentName: 'ApplicationsPage'
    },
    {
        path: 'resources',
        label: 'resources',
        modulePath: '/app/content/my-resources-page/MyResourcesPage',
        componentName: 'MyResourcesPage',
        hidden: true //!features.isMyResourcesEnabled
    }
];
