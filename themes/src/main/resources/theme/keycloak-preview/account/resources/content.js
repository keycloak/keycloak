
var content = [
    {
        id: 'personal-info',
        path: 'personal-info',
        label: 'personalInfoHtmlTitle',
        modulePath: '/app/content/account-page/AccountPage.js',
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
                modulePath: '/app/content/signingin-page/SigningInPage.js',
                componentName: 'SigningInPage',
            },
            {
                id: 'device-activity',
                path: 'security/device-activity',
                label: 'device-activity',
                modulePath: '/app/content/device-activity-page/DeviceActivityPage.js',
                componentName: 'DeviceActivityPage'
            },
            {
                id: 'linked-accounts',
                path: 'security/linked-accounts',
                label: 'linkedAccountsHtmlTitle',
                modulePath: '/app/content/linked-accounts-page/LinkedAccountsPage.js',
                componentName: 'LinkedAccountsPage',
                hidden: !features.isLinkedAccountsEnabled
            }
        ]
    },
    {
        id: 'applications',
        path: 'applications',
        label: 'applications',
        modulePath: '/app/content/applications-page/ApplicationsPage.js',
        componentName: 'ApplicationsPage'
    },
    {
        id: 'resources',
        path: 'resources',
        label: 'resources',
        modulePath: '/app/content/my-resources-page/MyResourcesPage.js',
        componentName: 'MyResourcesPage',
        hidden: !features.isMyResourcesEnabled
    }
];
