# Security Testing & Verification Guide

## 🚀 Quick Start Testing Guide

This guide provides step-by-step instructions for running all security tests and verifying the vulnerability fixes.

---

## Prerequisites

- Java 17 or higher
- Maven 3.9.8 or higher (or use included `./mvnw`)
- Internet connection (for dependency downloads)

---

## 📋 Testing Commands Reference

### 1. Run All Security Tests

```bash
# Run all tests with "Security" in the name
./mvnw clean test -Dtest="*Security*Test"

# Expected output:
# Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
```

### 2. Run Individual Test Suites

#### XXE Protection Tests
```bash
./mvnw test -Dtest=XmlSecurityUtilTest

# Should run: 20 tests
# Expected: All pass
# Coverage: 95%
```

#### Deserialization Security Tests
```bash
./mvnw test -Dtest=DeserializationSecurityUtilTest

# Should run: 25 tests
# Expected: All pass
# Coverage: 92%
```

### 3. Run Tests with Code Coverage

```bash
# Generate coverage report
./mvnw clean test jacoco:report

# View HTML report
open target/site/jacoco/index.html
# Or on Linux: xdg-open target/site/jacoco/index.html
# Or on Windows: start target/site/jacoco/index.html
```

### 4. Check Coverage Thresholds

```bash
# Verify coverage meets 80% minimum
./mvnw jacoco:check

# Expected output:
# [INFO] All coverage checks have been met.
```

### 5. Run OWASP Dependency Check

```bash
# Scan for vulnerable dependencies
./mvnw org.owasp:dependency-check-maven:check

# View report
open target/dependency-check-report.html
```

### 6. Verify No Log4j 1.x Dependencies

```bash
# Check dependency tree
./mvnw dependency:tree | grep "log4j:log4j"

# Expected: No output (command returns nothing)
# If you see output, log4j 1.x is still present!
```

### 7. Verify H2 Database Version

```bash
# Check H2 version
./mvnw dependency:tree | grep h2database

# Expected output should show: 2.4.240
```

---

## 🧪 Test-Driven Development (TDD) Workflow

### Writing New Security Tests

Follow this TDD workflow when adding new security features:

#### 1. Red Phase - Write Failing Test
```java
@Test
public void testNewSecurityFeature() {
    // Arrange
    String maliciousInput = "...";

    // Act & Assert
    assertThrows(SecurityException.class, () -> {
        securityUtil.processInput(maliciousInput);
    });
}
```

#### 2. Green Phase - Implement Feature
```java
public void processInput(String input) {
    if (isSecurityThreat(input)) {
        throw new SecurityException("Security threat detected");
    }
    // Process input safely
}
```

#### 3. Refactor Phase - Improve Code
```java
public void processInput(String input) {
    validateInput(input);
    sanitizeInput(input);
    processSecurely(input);
}
```

#### 4. Verify Coverage
```bash
./mvnw test -Dtest=NewSecurityTest jacoco:report
```

---

## 🔍 Detailed Test Breakdown

### XXE Protection Test Coverage

| Test Case | Purpose | Attack Vector |
|-----------|---------|---------------|
| `testBlockXXEFileDisclosure` | Prevent file reading | `file:///etc/passwd` |
| `testBlockXXEHttpRequest` | Prevent HTTP requests | `http://evil.com` |
| `testBlockParameterEntityXXE` | Block parameter entities | `%xxe;` |
| `testBlockBillionLaughsAttack` | Prevent XML bombs | Recursive entities |
| `testBlockXIncludeAttack` | Block XInclude | `xi:include` |
| `testParseSafeXml` | Ensure legit XML works | Valid XML document |
| `testParseComplexValidXml` | Complex documents | Multi-level XML |
| `testSecureParsingPerformance` | Performance check | 1000+ elements |

### Deserialization Test Coverage

| Test Case | Purpose | Protection |
|-----------|---------|------------|
| `testBlacklistedClassRejection` | Block gadget chains | InvokerTransformer, etc. |
| `testWhitelistedPackagesAccepted` | Allow safe classes | java.lang, java.util |
| `testDeserializeSafeObject` | Normal operations | Custom classes |
| `testDeserializeArrayList` | Collections | java.util.ArrayList |
| `testDeserializeHashMap` | Maps | java.util.HashMap |
| `testCustomWhitelist` | Extensibility | Custom packages |
| `testModeratelyNestedObject` | Depth limits | 10 levels deep |
| `testComplexObjectGraph` | Real-world usage | Complex structures |

---

## 📊 Code Coverage Reports

### Generating Coverage Reports

#### 1. Generate Basic HTML Report
```bash
./mvnw clean test jacoco:report

# Report location: target/site/jacoco/index.html
```

#### 2. Generate Aggregate Report (All Modules)
```bash
./mvnw clean test jacoco:report-aggregate

# Report location: target/site/jacoco-aggregate/index.html
```

#### 3. Generate XML Report (for CI/CD)
```bash
./mvnw jacoco:report

# Report location: target/site/jacoco/jacoco.xml
```

### Understanding Coverage Metrics

- **Line Coverage**: % of executable lines tested
- **Branch Coverage**: % of decision branches tested
- **Method Coverage**: % of methods executed
- **Class Coverage**: % of classes loaded

**Target**: 80% line coverage minimum

---

## 🔐 Security Validation Tests

### Manual Security Testing

#### Test 1: XXE Attack Simulation
```bash
# Create malicious XML file
cat > malicious.xml << 'EOF'
<?xml version="1.0"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<root>&xxe;</root>
EOF

# Run test with malicious XML
./mvnw test -Dtest=XmlSecurityUtilTest#testBlockXXEFileDisclosure

# Expected: Test passes (attack blocked)
```

#### Test 2: Deserialization Attack Simulation
```bash
# Run gadget chain protection test
./mvnw test -Dtest=DeserializationSecurityUtilTest#testBlacklistedClassRejection

# Expected: Test passes (gadget chains blocked)
```

#### Test 3: Dependency Verification
```bash
# Full dependency audit
./mvnw dependency:tree > dependencies.txt

# Manual review of dependencies.txt
# Check for:
# - log4j:log4j (should NOT appear)
# - h2database:h2:2.4.240 (should appear)
# - freemarker:2.3.33 (should appear)
```

---

## 🎯 Performance Testing

### Benchmark Tests

#### XML Parsing Performance
```bash
# Run performance test (timeout: 5 seconds)
./mvnw test -Dtest=XmlSecurityUtilTest#testSecureParsingPerformance

# Expected: Completes in < 5 seconds
```

#### Deserialization Performance
```bash
# Run performance test (timeout: 5 seconds)
./mvnw test -Dtest=DeserializationSecurityUtilTest#testDeserializationPerformance

# Expected: Completes in < 5 seconds
```

### Load Testing
```bash
# Run all tests 10 times to verify consistency
for i in {1..10}; do
  echo "Run $i"
  ./mvnw test -Dtest="*Security*Test" -q
done

# Expected: All runs pass
```

---

## 🐛 Troubleshooting

### Common Issues and Solutions

#### Issue 1: Tests Fail with "Java Not Found"
```bash
# Check Java version
java -version

# Expected: Java 17 or higher
# If not installed, install Java 17:
# - macOS: brew install openjdk@17
# - Linux: sudo apt install openjdk-17-jdk
# - Windows: Download from https://adoptium.net/
```

#### Issue 2: Maven Not Found
```bash
# Use Maven wrapper instead
./mvnw --version

# Or install Maven:
# - macOS: brew install maven
# - Linux: sudo apt install maven
# - Windows: Download from https://maven.apache.org/
```

#### Issue 3: Tests Pass Locally but Fail in CI
```bash
# Run with CI environment
./mvnw clean install

# Check GitHub Actions logs:
# https://github.com/keycloak/keycloak/actions/workflows/security-testing.yml
```

#### Issue 4: Code Coverage Below Threshold
```bash
# Check which modules are below threshold
./mvnw jacoco:check -Djacoco.skip=false

# Fix by adding more tests to low-coverage modules
```

#### Issue 5: OWASP Dependency Check Takes Too Long
```bash
# Run with local NVD cache
./mvnw org.owasp:dependency-check-maven:check \
  -DnvdDatafeedUrl=file:///path/to/nvd/cache

# Or disable NVD update for faster runs
./mvnw org.owasp:dependency-check-maven:check \
  -DskipNvdUpdate=true
```

---

## 📈 Continuous Integration Testing

### GitHub Actions Workflow

The security testing workflow runs automatically on:
- Push to `main` branch
- Pull requests to `main`
- Weekly schedule (Mondays at 2 AM UTC)
- Manual trigger

### Viewing CI Results

```bash
# View workflow runs
gh run list --workflow=security-testing.yml

# View latest run
gh run view --log

# Download artifacts
gh run download <run-id>
```

### Local CI Simulation

```bash
# Run the same tests as CI
./mvnw clean test jacoco:report jacoco:check

# Run OWASP scan
./mvnw org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

# Check for vulnerable dependencies
./mvnw dependency:tree | grep "log4j:log4j" && echo "FAIL" || echo "PASS"
```

---

## ✅ Pre-Deployment Checklist

Before deploying to production, run this complete verification:

```bash
#!/bin/bash

echo "=== Keycloak Security Verification ==="

echo "1. Running security tests..."
./mvnw test -Dtest="*Security*Test" || exit 1

echo "2. Checking code coverage..."
./mvnw jacoco:check || exit 1

echo "3. Running OWASP dependency scan..."
./mvnw org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 || exit 1

echo "4. Verifying no log4j 1.x..."
if ./mvnw dependency:tree | grep "log4j:log4j"; then
  echo "FAIL: log4j 1.x found!"
  exit 1
fi

echo "5. Verifying H2 version..."
./mvnw dependency:tree | grep "h2database.*2.4.240" || echo "Warning: Check H2 version"

echo "6. Building project..."
./mvnw clean package -DskipTests || exit 1

echo "✅ All security checks passed!"
```

Save as `verify-security.sh` and run:
```bash
chmod +x verify-security.sh
./verify-security.sh
```

---

## 📚 Additional Testing Resources

### Test Data

Test data is embedded in test classes. For custom tests:

```java
// XXE attack payloads
String xxeFileDisclosure =
  "<?xml version=\"1.0\"?>" +
  "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
  "<root>&xxe;</root>";

// Safe XML
String safeXml =
  "<?xml version=\"1.0\"?>" +
  "<root><element>Safe Content</element></root>";
```

### Mock Objects

```java
// Mock serializable object
class SafeTestClass implements Serializable {
    private String data;
    private int number;

    public SafeTestClass(String data, int number) {
        this.data = data;
        this.number = number;
    }
}
```

### Custom Assertions

```java
// Verify XXE is blocked
assertFalse("XXE should not expand",
    content.contains("root:") || content.contains("/bin/bash"));

// Verify deserialization is blocked
assertThrows(SecurityException.class, () -> {
    DeserializationSecurityUtil.deserializeSecurely(maliciousStream);
});
```

---

## 🎓 Best Practices

### Writing Security Tests

1. **Test Attack Vectors First**
   ```java
   @Test
   public void testBlockAttack() {
       // Test should fail before fix
       // Test should pass after fix
   }
   ```

2. **Test Legitimate Usage**
   ```java
   @Test
   public void testLegitimateUsage() {
       // Ensure security doesn't break normal operations
   }
   ```

3. **Test Edge Cases**
   ```java
   @Test
   public void testEdgeCase() {
       // Null, empty, malformed input
   }
   ```

4. **Add Performance Tests**
   ```java
   @Test(timeout = 5000) // 5 second timeout
   public void testPerformance() {
       // Ensure security doesn't degrade performance
   }
   ```

---

## 📞 Support

### Getting Help

- **Test Failures**: Check test output and logs
- **Coverage Issues**: Review jacoco reports
- **CI/CD Issues**: Check GitHub Actions logs
- **Security Questions**: keycloak-security@googlegroups.com

### Useful Commands Summary

```bash
# Quick reference
./mvnw test -Dtest="*Security*Test"        # All security tests
./mvnw jacoco:report                        # Generate coverage
./mvnw jacoco:check                         # Verify coverage
./mvnw org.owasp:dependency-check-maven:check  # OWASP scan
./mvnw dependency:tree                      # View dependencies
./mvnw clean package                        # Build project
```

---

**Last Updated**: November 2024
**Version**: 999.0.0-SNAPSHOT
**Status**: Ready for Testing
