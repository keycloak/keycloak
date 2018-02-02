var privateConfig = require('./private.conf.js').config;

var testUrl = '/test/page/test.html',
    viewports = [[1280, 1024], [640, 480], [320, 640]];

var capabilities = [
    {
        browser: 'ie',
        browser_version: '11',
        os: 'windows',
        os_version: '10'
    },
    {
        browser: 'ie',
        browser_version: '10',
        os: 'windows',
        os_version: '7'
    },
    {
        browser: 'ie',
        browser_version: '9',
        os: 'windows',
        os_version: '7'
    },
    {
        browser: 'ie',
        browser_version: '8',
        os: 'windows',
        os_version: '7',
        viewports: [[1280, 1024]]
    },
    {
        browserName: 'chrome',
        os: 'windows',
        os_version: '8'
    },
    {
        browserName: 'firefox',
        os: 'windows',
        os_version: '8'
    },
    {
        browser: 'safari',
        browser_version: '8'
    },
    {
        browser: 'safari',
        browser_version: '7.1'
    },
    {
        browser: 'safari',
        browser_version: '6.2'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 6',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 6',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 5S',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 5S',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 4S',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 4S',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 5S',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 5S',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPad',
        device: 'iPad 4th',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPad',
        device: 'iPad 4th',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPad',
        device: 'iPad 3rd',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPad',
        device: 'iPad 3rd',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'iPad',
        device: 'iPad 2nd',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPad',
        device: 'iPad 2nd',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'android',
        device: 'Samsung Galaxy S5',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'android',
        device: 'Samsung Galaxy S5',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'android',
        device: 'Samsung Galaxy Tab 4 10.1',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'android',
        device: 'Samsung Galaxy Tab 4 10.1',
        deviceOrientation: 'landscape'
    }
];

for (var i = 0; i < capabilities.length; i += 1) {
    var capability = capabilities[i];
    capability['browserstack.debug'] = true;
    capability['urls'] = capability['urls'] || [testUrl];

    if (!capability['deviceOrientation']) {
        capability['viewports'] = capability['viewports'] || viewports;
        capability['resolution'] = capability['resolution'] || '1680x1050';
    }
}

exports.config = {
    user: privateConfig.user,
    key: privateConfig.key,
    capabilities: capabilities,
    specs: [
        './test/specs/webdriver.spec.js'
    ],
    logLevel: 'silent',
    coloredLogs: true,
    waitforTimeout: 60000,
    framework: 'jasmine',
    reporter: 'spec',
    pauseOnFail: false,
    jasmineNodeOpts: {
        defaultTimeoutInterval: 60000
    }
};
