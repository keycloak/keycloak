package org.keycloak.it.cli;


/**
 * Used to specify the output directory for the received / to-be-approved outputs of this packages tests.
 * In our case they should be stored under resources/clitest/approvals or resources/rawdist/approvals depending
 * on the runtype of the tests (@DistributionTest in Raw mode, or @CLITest, leading to either using "kc.sh"
 * or "java -jar $KEYCLOAK_HOME/lib/quarkus-run.jar" as command in the usage output).
 *
 * Note: Creates the directories if they don't exist yet.
 * **/
public class PackageSettings {

    public String UseApprovalSubdirectory = "approvals/cli/help";
    public String ApprovalBaseDirectory = "../resources";
}
