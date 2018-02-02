var privateConfig = require('./private.conf.js').config;

var testUrl = '/test/page/test.html',
    viewports = [[1280, 1024], [640, 480], [320, 640]];

var capabilities = [
    {
        browser: 'ie',
        browser_version: '8',
        os: 'windows',
        os_version: '7',
        viewports: [[1280, 1024]]
    },
    {
        browser: 'safari',
        browser_version: '8'
    },
    {
        browser: 'iPhone',
        device: 'iPhone 6',
        deviceOrientation: 'portrait'
    },
    {
        browser: 'iPad',
        device: 'iPad 4th',
        deviceOrientation: 'landscape'
    },
    {
        browser: 'android',
        device: 'Samsung Galaxy S5',
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
        capability['resolution'] = capability['resolution'] || '1600x1200';
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
