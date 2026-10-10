# SCA report processor

Tool to parse reports from different SCA tools with support for:

* Combines multiple reports into a single vendor neutral report
  * Aligning detected vulnerabilities across multiple release streams
  * Deduplicating detected vulnerabilities
* Supports SNYK and Trivy
* Retrieving additional CVE metadata from cve.org
* Identifying where a Maven dependency is declared
* Maintaining GitHub Issues associated with the detected vulnerabilities
* CVE ignore list

The reason this tool exists is to provide vendor neutrality and make it easier to scan with multiple SCA tools for
increased coverage.

## Scanning for vulnerabilities

First step to using the tool is to get some reports:

Trivy
```
trivy fs --scanners vuln --output <file>
```

Snyk:
```
snyk test --json-file-output=<file>
```

## Running the tool

The tool can scan a single report or a directory with multiple reports. 

For multiple release stream support the report directory should contain a child directory per-release stream with the 
naming scheme `reports-refs_heads_<branch name>`. For example:

```
reports
├── reports-refs_heads_main
│   ├── snyk-report.json
│   └── trivy-report.json
├── reports-refs_heads_release_26.6
│   ├── snyk-report.json
    └── trivy-report.json
└── reports-refs_heads_release_26.7
    ├── snyk-report.json
    └── trivy-report.json
```

When using the tool to update GitHub Issues it is important to have reports for all active release streams, with all
tools. Otherwise, the tool may update managed GitHub Issues wrongly.

Run the tool with:
```
./vuln-to-issues.sh [options] <reports path>
```

Supported options are:

| Name                        | Description                                                                   |
|-----------------------------|-------------------------------------------------------------------------------|
| `--repository=<owner/name>` | GitHub Repository for GitHub Issues (supports GITHUB_REPOSITORY env variable) |
| `--update-issues`           | Create or Update GitHub Issues                                                |
| `--ignore=<path>`           | CVE ignore file                                                               |
| `--output=<path>`           | Write report to file if set (supports GITHUB_STEP_SUMMARY env variable)       |
| `--stream=<stream>          | Set stream name when not used to scan multi-stream report directories         |
