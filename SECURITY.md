# Vulnerability Disclosure Policy

As a U.S. government agency, the General Services Administration (GSA) takes seriously our responsibility to protect the public's information, including financial and personal information, from unwarranted disclosure.

Security researchers should feel comfortable reporting vulnerabilities discovered, as defined in this policy, to afford GSA the opportunity to remediate the findings for the purpose of ensuring confidentiality, so we can fix them and keep our information safe.

This policy describes what systems and types of research are covered under this policy, how to send us vulnerability reports, and how long we ask security researchers to wait before publicly disclosing vulnerabilities.

## Scope

This policy applies to the systems in the Scopes section identified at **HackerOne**.

Any services not expressly listed above, such as any connected services, are excluded from scope and are not authorized for testing. Additionally, vulnerabilities found in non-federal systems from our vendors fall outside of this policy's scope and should be reported directly to the vendor according to their disclosure policy (if any). If you aren't sure whether a system or endpoint is in scope or not, contact us via gsa-vulnerability-reports@gsa.gov before starting your research.

The following test types are not authorized:
* User interface bugs or typos
* Network denial of service (DoS or DDoS) tests
* Physical testing (e.g. office access, open doors, tailgating), social engineering (e.g. phishing, vishing), or any other non-technical vulnerability testing
* Brute Force Attacks against login interfaces

If you encounter any of the below on our systems while testing within the scope of this policy, stop your test and notify us immediately. Disclosure of the following may not be made to any third party:
* Personally identifiable information (PII)
* Financial information (e.g. credit card or bank account numbers)
* Proprietary information or trade secrets of companies of any party

## Guidelines

Security researchers shall:
* Make every effort to avoid privacy violations, degradation of user experience, disruption to production systems, and destruction or manipulation of data
* Only use exploits to the extent necessary to confirm a vulnerability. Do not use an exploit to compromise or exfiltrate data, establish command line access and/or persistence, or use the exploit to "pivot" to other systems. Once you've established that a vulnerability exists, or encountered any of the sensitive data outlined above, you must stop your test and notify us immediately
* Keep confidential any information about discovered vulnerabilities for up to 90 calendar days after you have notified GSA. For details, please review Coordinated Disclosure

GSA is committed to acknowledging receipt of the report within 2 business days via the HackerOne platform.

## Legal

You must comply with all applicable Federal, State, and local laws in connection with your security research activities or other participation in this vulnerability disclosure program.

GSA does not authorize, permit, or otherwise allow (expressly or impliedly) any person, including any individual, group of individuals, consortium, partnership, or any other business or legal entity to engage in any security research or vulnerability or threat disclosure activity that is inconsistent with this policy or the law. If you engage in any activities that are inconsistent with this policy or the law, you may be subject to criminal and/or civil liabilities.

To the extent that any security research or vulnerability disclosure activity involves the networks, systems, information, applications, products, or services of a non-GSA entity (e.g., other Federal departments or agencies; State, local, or tribal governments; private sector companies or persons; employees or personnel of any such entities; or any other such third party), that non-GSA third party may independently determine whether to pursue legal action or remedies related to such activities.

If you conduct your security research and vulnerability disclosure activities in accordance with the restrictions and guidelines set forth in this policy, (1) GSA will not initiate or recommend any law enforcement or civil lawsuits related to such activities, and (2) in the event of any law enforcement or civil action brought by anyone other than GSA, GSA will communicate as appropriate, in the absence of any legal restriction on GSA's ability to so communicate, that your activities were conducted pursuant to and in compliance with this policy.

## Reporting a vulnerability

You can email vulnerability reports to gsa-vulnerability-reports@gsa.gov or submit them via the **HackerOne Submit** portal.

_Note: We do not support PGP-encrypted emails. Do not share sensitive information through email. If you believe it is necessary to share sensitive information with us, please indicate as such on the report and GSA will reach out to establish a more secure method._

Reports should include:
* Description of the location and potential impact of the vulnerability
* A detailed description of the steps required to reproduce the vulnerability. Proof of concept (POC) scripts, screenshots, and screen captures are all helpful. Please use extreme care to properly label and protect any exploit code
* Any technical information and related materials we would need to reproduce the issue

Please keep your vulnerability reports current by sending us any new information as it becomes available. We may share your vulnerability reports with US-CERT, as well as any affected vendors or open source projects.

## Coordinated disclosure

GSA is committed to patching vulnerabilities within 90 days or less and disclosing the details of those vulnerabilities when patches are published. We believe that public disclosure of vulnerabilities is an essential part of the vulnerability disclosure process, and that one of the best ways to make software better is to enable everyone to learn from each other's mistakes.

At the same time, we believe that disclosure in absence of a readily available patch tends to increase risk rather than reduce it, and so we request that you refrain from sharing your report with others while we work on our patch. If you believe there are others that should be informed of your report before the patch is available, please let us know so we can make arrangements.

We may want to coordinate an advisory with you to be published simultaneously with the patch, but you are also welcome to self-disclose if you prefer. By default, we prefer to disclose everything, but we will never publish information about you or our communications with you without your permission. In some cases, we may also have some sensitive information that should be redacted, and so please check with us before self-disclosing.
