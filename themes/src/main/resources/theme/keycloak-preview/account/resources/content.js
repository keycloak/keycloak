
var content = [
    {
        id: 'personal-info',
        path: 'personal-info',
        label: 'personalInfoHtmlTitle',
        modulePath: '/content/account-page/AccountPage.js',
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
                modulePath: '/content/signingin-page/SigningInPage.js',
                componentName: 'SigningInPage',
            },
            {
                id: 'device-activity',
                path: 'security/device-activity',
                label: 'device-activity',
                modulePath: '/content/device-activity-page/DeviceActivityPage.js',
                componentName: 'DeviceActivityPage'
            },
            {
                id: 'linked-accounts',
                path: 'security/linked-accounts',
                label: 'linkedAccountsHtmlTitle',
                modulePath: '/content/linked-accounts-page/LinkedAccountsPage.js',
                componentName: 'LinkedAccountsPage',
                hidden: !features.isLinkedAccountsEnabled
            }
        ]
    },
    {
        id: 'applications',
        path: 'applications',
        label: 'applications',
        modulePath: '/content/applications-page/ApplicationsPage.js',
        componentName: 'ApplicationsPage'
    },
    {
        id: 'resources',
        path: 'resources',
        label: 'resources',
        modulePath: '/content/my-resources-page/MyResourcesPage.js',
        componentName: 'MyResourcesPage',
        hidden: !features.isMyResourcesEnabled
    },
    {
        id: 'deleteAccount',
        path: 'deleteAccount',
        label: 'Delete Account',
        modulePath: '/content/delete-account-page/DeleteAccountPage.js',
        componentName: 'DeleteAccountPage',
        hidden: !features.deleteAccountAllowed
    }
];
