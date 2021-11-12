var testUrl = '/test/page/test.html',
    hasIE = /^win/.test(process.platform),
    viewports = [[1280, 1024], [640, 480], [320, 640]];

var capabilities = [
    {
        browserName: 'chrome'
    },
    {
        browserName: 'firefox'
    }
];

if (hasIE) {
    capabilities = capabilities.concat([
        {
            browserName: 'internet explorer',
            urls: [testUrl, testUrl + '?ie=9', testUrl + '?ie=10']
        },
        {
            browserName: 'internet explorer',
            urls: [testUrl + '?ie=8'],
            viewports: [[1280, 1024]]
        }
    ]);
}

for (var i = 0; i < capabilities.length; i += 1) {
    var capability = capabilities[i];
    capability['urls'] = capability['urls'] || [testUrl];
    capability['viewports'] = capability['viewports'] || viewports;
}

exports.config = {
    capabilities: capabilities,
    specs: [
        './test/specs/webdriver.spec.js'
    ],
    logLevel: 'silent',
    coloredLogs: true,
    waitforTimeout: 99999999,
    framework: 'jasmine',
    reporter: 'spec',
    pauseOnFail: true,
    jasmineNodeOpts: {
        defaultTimeoutInterval: 99999999
    }
};
