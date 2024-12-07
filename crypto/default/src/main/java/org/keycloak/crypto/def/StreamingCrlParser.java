/*
 * Copyright 2013-2023 xipki
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.crypto.def;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.bc.BcContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcDSAContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcECContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;
import org.bouncycastle.util.Arrays;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;

/**
 * Originally adapted from xipki/commons CrlStreamParser
 *
 * @author Lijun Liao (xipki)
 * @author Scott Tustison
 * @author Joshua Smith
 */
public class StreamingCrlParser extends X509CRL {

  private static final int TAG_CONSTRUCTED_SEQUENCE = BERTags.CONSTRUCTED | BERTags.SEQUENCE;

  private static final Map<String, BcContentVerifierProviderBuilder> VERIFIER_PROVIDER_BUILDER = new HashMap<>();

  private static final DigestAlgorithmIdentifierFinder DIGEST_ALGORITHM_IDENTIFIER_FINDER
          = new DefaultDigestAlgorithmIdentifierFinder();

  private final File crlFile;

  private int version;

  private X500Name issuer;

  private Date thisUpdate;

  private Date nextUpdate;

  private AlgorithmIdentifier algorithmIdentifier;

  private byte[] signature;

  private Extensions crlExtensions;

  private int firstRevokedCertificateOffset;

  private int revokedCertificatesEndIndex;

  private int tbsCertListOffset;

  private int tbsCertListLength;

  private int tbsCertListEndIndex;

  private AlgorithmIdentifier tbsSignature;

  public StreamingCrlParser(File crlFile) throws IOException {
    this.crlFile = crlFile;
    initParser(crlFile);
  }

  private void initParser(File crlFile) throws IOException {
    try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(Objects.requireNonNull(crlFile).toPath()))) {
      ParseResult parseResult = new ParseResult();
      parseResult.setTag(checkEncoding(inputStream));
      parseResult.setOffset(0);
      parseResult.setByteLength(0);

      setCertificateListOffset(parseResult, inputStream);

      setTbsCertListLengthAndIndex(parseResult, inputStream);

      setVersion(parseResult, inputStream);

      setTbsSignature(parseResult, inputStream);

      setIssuer(parseResult, inputStream);

      setThisUpdate(parseResult, inputStream);

      setNextUpdate(parseResult, inputStream);

      setRevokedIndexAndOffset(parseResult, inputStream);

      setCrlExtensions(parseResult, inputStream);

      setAlgIdentifierAndSignature(inputStream);
    }
  }

  private void setNextUpdate(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    parseResult.setTag(peekTag(inputStream));
    if (parseResult.getTag() != TAG_CONSTRUCTED_SEQUENCE) {
      inputStream.reset();
      this.nextUpdate = readTime(parseResult, inputStream);
      parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());
      parseResult.setTag(peekTag(inputStream));
    } else {
      this.nextUpdate = null;
    }
    parseResult.setOffset(parseResult.getOffset() + 1);
  }

  private void setAlgIdentifierAndSignature(BufferedInputStream inputStream) throws IOException {
    byte[] bytes;
    bytes = readBlock(TAG_CONSTRUCTED_SEQUENCE, inputStream, "signatureAlgorithm");
    this.algorithmIdentifier = AlgorithmIdentifier.getInstance(bytes);
    if (!tbsSignature.equals(this.algorithmIdentifier)) {
      throw new IllegalArgumentException("algorithmIdentifier != tbsCertList.signature");
    }

    bytes = readBlock(BERTags.BIT_STRING, inputStream, "signature");
    this.signature = ASN1BitString.getInstance(bytes).getBytes();
  }

  private void setCrlExtensions(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    byte[] bytes;
    int crlExtensionsTag = BERTags.TAGGED | BERTags.CONSTRUCTED;

    Extensions extns = null;
    if (parseResult.getOffset() < tbsCertListEndIndex) {
      while (parseResult.getOffset() < tbsCertListEndIndex) {
        int tag = markAndReadTag(inputStream);
        parseResult.setOffset(parseResult.getOffset() + 1);

        int length = readLength(parseResult, inputStream);
        parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());

        if (tag != crlExtensionsTag) {
          skip(inputStream, length);
          parseResult.setOffset(parseResult.getOffset() + length);
        } else {
          inputStream.mark(1);
          // Read extensions bytes, optional
          bytes = readBlock(TAG_CONSTRUCTED_SEQUENCE, inputStream, "crlExtensions");
          parseResult.setOffset(parseResult.getOffset() + bytes.length);
          extns = Extensions.getInstance(bytes);
        }
      }
    }

    this.crlExtensions = extns;
  }

  private void setRevokedIndexAndOffset(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    if (parseResult.getOffset() < tbsCertListLength && TAG_CONSTRUCTED_SEQUENCE == parseResult.getTag()) {
      markAndReadTag(inputStream);
      int revokedCertificatesOffset = parseResult.getOffset();
      int revokedCertificatesLength = readLength(parseResult, inputStream);
      parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());

      this.revokedCertificatesEndIndex = revokedCertificatesOffset + revokedCertificatesLength;
      this.firstRevokedCertificateOffset = parseResult.getOffset();

      // Skip the revokedCertificates
      skip(inputStream, revokedCertificatesLength);
      parseResult.setOffset(parseResult.getOffset() + revokedCertificatesLength);
    } else {
      this.revokedCertificatesEndIndex = -1;
      this.firstRevokedCertificateOffset = -1;
    }
  }

  private void setThisUpdate(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    this.thisUpdate = readTime(parseResult, inputStream);
    parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());
  }

  private void setIssuer(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    byte[] bytes;
    bytes = readBlock(TAG_CONSTRUCTED_SEQUENCE, inputStream, "tbsCertList.issuer");
    parseResult.setOffset(parseResult.getOffset() + bytes.length);
    this.issuer = X500Name.getInstance(bytes);
  }

  private void setTbsSignature(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    byte[] bytes;
    bytes = readBlock(TAG_CONSTRUCTED_SEQUENCE, inputStream, "tbsCertList.signature");
    parseResult.setOffset(parseResult.getOffset() + bytes.length);

    tbsSignature = AlgorithmIdentifier.getInstance(bytes);
  }

  private void setVersion(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    parseResult.setTag(peekTag(inputStream));
    inputStream.reset();
    byte[] bytes;
    if (parseResult.getTag() == BERTags.INTEGER) {
      // Set optional field version
      bytes = readBlock(inputStream, "tbsCertList.version");
      parseResult.setOffset(parseResult.getOffset() + bytes.length);

      this.version = ASN1Integer.getInstance(bytes).getValue().intValue();
    } else {
      // Default version of v1 used
      this.version = 0;
    }
  }

  private void setTbsCertListLengthAndIndex(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    parseResult.setTag(markAndReadTag(inputStream));
    parseResult.setOffset(parseResult.getOffset() + 1);
    assertTag(TAG_CONSTRUCTED_SEQUENCE, parseResult.getTag(), "tbsCertList");

    tbsCertListLength = readLength(parseResult, inputStream);
    parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());

    // Set ending index of tbsCertList
    tbsCertListEndIndex = parseResult.getOffset() + tbsCertListLength;
  }

  private void setCertificateListOffset(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    assertTag(TAG_CONSTRUCTED_SEQUENCE, parseResult.getTag(), "CertificateList");

    parseResult.setOffset(parseResult.getOffset() + 1);

    // Read length of CertificateList
    readLength(parseResult, inputStream);
    parseResult.setOffset(parseResult.getOffset() + parseResult.getByteLength());

    // Set cert list offset
    tbsCertListOffset = parseResult.getOffset();
  }

  private int checkEncoding(BufferedInputStream inputStream) throws IOException {
    int tag = markAndReadTag(inputStream);
    if (tag == '-') {
      throw new IllegalArgumentException("The CRL is not DER encoded.");
    }
    return tag;
  }

  @Override
  public void verify(PublicKey key) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
    try {
      boolean verified = verifySignature(key);
      if (!verified) {
        throw new CRLException("CRL public key could not be verified.");
      }
    } catch (IOException e) {
      throw new CRLException("CRL public key could not be verified.", e);
    }
  }

  @Override
  public void verify(PublicKey key, String sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
    throw new UnsupportedOperationException("Use verify(PublicKey) instead.");
  }

  @Override
  public Set<? extends X509CRLEntry> getRevokedCertificates() {
    //CRL entries are 20 to 40 bytes in length depending on if they have extensions or not
    //we will use 30 as an average size of the CRL to estimate an initial size of the Set
    long length = crlFile.length();
    int initialSize = Math.toIntExact(length / 30);
    Set<X509CRLEntry> certSet = new HashSet<>(initialSize);
    try {
      try (RevokedCertificateIterator revokedCertificateIterator = revokedCertificates()) {
        while(revokedCertificateIterator.hasNext()) {
          certSet.add(revokedCertificateIterator.next());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("Unable to read the CRL file: %s", crlFile));
    }
    return certSet;
  }

  private boolean verifySignature(PublicKey publicKey) throws IOException {
    try {
      ContentVerifierProvider cvp = getContentVerifierProvider(publicKey);
      ContentVerifier verifier = cvp.get(algorithmIdentifier);
      try (OutputStream sigOut = verifier.getOutputStream()) {
        try (InputStream crlStream = Files.newInputStream(crlFile.toPath())) {
          skip(crlStream, tbsCertListOffset);

          int remainingLength = tbsCertListEndIndex - tbsCertListOffset;
          byte[] buffer = new byte[1024];

          while (true) {
            int count = crlStream.read(buffer);
            if (count == -1) {
              break;
            } else if (count > 0) {
              if (count <= remainingLength) {
                sigOut.write(buffer, 0, count);
                remainingLength -= count;
              } else {
                sigOut.write(buffer, 0, remainingLength);
                remainingLength = 0;
              }
            }

            if (remainingLength == 0) {
              break;
            }
          }

          if (remainingLength != 0) {
            throw new IOException("Failed to read entire tbsCertList.");
          }
        }
      }

      return verifier.verify(this.getSignature());
    } catch (InvalidKeyException | OperatorCreationException ex) {
      return false;
    }
  }

  private ContentVerifierProvider getContentVerifierProvider(
          PublicKey publicKey) throws InvalidKeyException {
    String keyAlg = publicKey.getAlgorithm().toUpperCase();

    BcContentVerifierProviderBuilder builder = VERIFIER_PROVIDER_BUILDER.get(keyAlg);

    if (builder == null) {
        switch (keyAlg) {
            case "RSA":
                builder = new BcRSAContentVerifierProviderBuilder(DIGEST_ALGORITHM_IDENTIFIER_FINDER);
                break;
            case "DSA":
                builder = new BcDSAContentVerifierProviderBuilder(DIGEST_ALGORITHM_IDENTIFIER_FINDER);
                break;
            case "EC":
            case "ECDSA":
                builder = new BcECContentVerifierProviderBuilder(DIGEST_ALGORITHM_IDENTIFIER_FINDER);
                break;
            default:
                throw new InvalidKeyException(String.format("Unknown key algorithm for the public key %s", keyAlg));
        }
      VERIFIER_PROVIDER_BUILDER.put(keyAlg, builder);
    }

    AsymmetricKeyParameter keyParam = generatePublicKeyParameter(publicKey);
    try {
      return builder.build(keyParam);
    } catch (OperatorCreationException ex) {
      throw new InvalidKeyException(String.format("Could not build ContentVerifierProvider: %s", ex.getMessage()), ex);
    }
  }

  private static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey key) throws InvalidKeyException {
    if (key instanceof RSAPublicKey) {
      RSAPublicKey rsaKey = (RSAPublicKey) key;
      return new RSAKeyParameters(false, rsaKey.getModulus(), rsaKey.getPublicExponent());
    } else if (key instanceof ECPublicKey) {
      return ECUtil.generatePublicKeyParameter(key);
    } else if (key instanceof DSAPublicKey) {
      return DSAUtil.generatePublicKeyParameter(key);
    } else {
      throw new InvalidKeyException(String.format("Unknown key %s", key.getClass().getName()));
    }
  }

  private byte[] readBlock(BufferedInputStream inputStream, String name) throws IOException {
    return readBlock(Integer.MAX_VALUE, inputStream, name);
  }

  private byte[] readBlock(int expectedTag, BufferedInputStream inputStream, String name) throws IOException {
    inputStream.mark(10);
    int tag = inputStream.read();
    if (expectedTag != Integer.MAX_VALUE) {
      assertTag(expectedTag, tag, name);
    }

    ParseResult parseResult = new ParseResult();
    parseResult.setByteLength(0);
    int length = readLength(parseResult, inputStream);
    inputStream.reset();

    byte[] bytes = new byte[1 + parseResult.getByteLength() + length];
    if (bytes.length != inputStream.read(bytes)) {
      throw new IOException(String.format("Error reading block, length doesn't match: %s", name));
    }
    return bytes;
  }

  private int markAndReadTag(InputStream inputStream) throws IOException {
    inputStream.mark(10);
    return inputStream.read();
  }

  private int peekTag(InputStream inputStream) throws IOException {
    inputStream.mark(1);
    int tag = inputStream.read();
    inputStream.reset();
    return tag;
  }

  private int readLength(ParseResult parseResult, InputStream inputStream) throws IOException {
    int b = inputStream.read();
    if ((b & 0x80) == 0) {
      parseResult.setByteLength(1);
      return b;
    } else {
      byte[] lengthBytes = new byte[b & 0x7F];
      if (lengthBytes.length > 4) {
        throw new IOException("Length of bytes is too long.");
      }
      parseResult.setByteLength(1 + lengthBytes.length);

      if (lengthBytes.length > inputStream.read(lengthBytes)) {
        throw new IOException("Not enough data to read.");
      }

      int length = 0xFF & lengthBytes[0];
      for (int i = 1; i < lengthBytes.length; i++) {
        length = (length << 8) + (0xFF & lengthBytes[i]);
      }
      return length;
    }
  }

  private void assertTag(int expectedTag, int tag, String name) {
    if (expectedTag != tag) {
      throw new IllegalArgumentException(
              String.format("Invalid %s: tag is %d, but not expected %d", name, tag, expectedTag));
    }
  }

  private Date readTime(ASN1Encodable  obj) {
    return  (obj instanceof Time) ? ((Time) obj).getDate()
            : (obj instanceof org.bouncycastle.asn1.cms.Time) ? ((org.bouncycastle.asn1.cms.Time) obj).getDate()
            : Time.getInstance(obj).getDate();
  }

  private Date readTime(ParseResult parseResult, BufferedInputStream inputStream) throws IOException {
    int tag = peekTag(inputStream);
    byte[] bytes = readBlock(inputStream, "tbsCertList.thisUpdate");
    parseResult.setByteLength(bytes.length);
    try {
      if (tag == BERTags.UTC_TIME) {
        return DERUTCTime.getInstance(bytes).getDate();
      } else if (tag == BERTags.GENERALIZED_TIME) {
        return DERGeneralizedTime.getInstance(bytes).getDate();
      } else {
        throw new IllegalArgumentException(String.format("Invalid tag for tbsCertList.thisUpdate: %s", tag));
      }
    } catch (ParseException ex) {
      throw new IllegalArgumentException("Error parsing time", ex);
    }
  }

  private void skip(InputStream inputStream, long count) throws IOException {
    long remaining = count;
    while (remaining > 0) {
      remaining -= inputStream.skip(remaining);
    }
  }

  public RevokedCertificateIterator revokedCertificates() throws IOException {
    return new RevokedCertificateIterator();
  }

  public int getVersion() {
    return version;
  }

  @Override
  public Principal getIssuerDN() {
    try {
      return new X500Principal(issuer.getEncoded());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public X500Principal getIssuerX500Principal() {
    return (X500Principal) getIssuerDN();
  }

  public X500Name getIssuer() {
    return issuer;
  }

  @Override
  public Date getThisUpdate() {
    return thisUpdate;
  }

  @Override
  public Date getNextUpdate() {
    return nextUpdate;
  }

  @Override
  public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
    throw new UnsupportedOperationException("Returning individual certificates is not supported.");
  }

  @Override
  public byte[] getTBSCertList() {
    throw new UnsupportedOperationException("Getting the cert list bytes is not supported.");
  }

  @Override
  public byte[] getSignature() {
    return Arrays.copyOf(signature, signature.length);
  }

  @Override
  public String getSigAlgName() {
    throw new UnsupportedOperationException("Getting the signature algorithm name is not supported.");
  }

  @Override
  public String getSigAlgOID() {
    throw new UnsupportedOperationException("Getting the signature algorithm OID is not supported.");
  }

  @Override
  public byte[] getSigAlgParams() {
    throw new UnsupportedOperationException("Getting the signature algorithm parameters is not supported.");
  }

  @Override
  public byte[] getEncoded() throws CRLException {
    throw new UnsupportedOperationException("Getting the encoded CRL is not supported.");
  }

  @Override
  public String toString() {
    return crlFile.toString();
  }

  @Override
  public boolean isRevoked(Certificate cert) {
    throw new UnsupportedOperationException("Checking revocation status of a single certificate is not supported.");
  }

  @Override
  public boolean hasUnsupportedCriticalExtension() {
    throw new UnsupportedOperationException("Checking extensions is not supported.");
  }

  @Override
  public Set<String> getCriticalExtensionOIDs() {
    throw new UnsupportedOperationException("Checking extensions is not supported.");
  }

  @Override
  public Set<String> getNonCriticalExtensionOIDs() {
    throw new UnsupportedOperationException("Checking extensions is not supported.");
  }

  @Override
  public byte[] getExtensionValue(String oid) {
    throw new UnsupportedOperationException("Getting extension bytes is not supported.");
  }

  public static class ParseResult {
    private int tag;

    private int offset;

    private int byteLength;

    void setTag(int tag) {
      this.tag = tag;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }

    public void setByteLength(int byteLength) {
      this.byteLength = byteLength;
    }

    public int getTag() {
      return tag;
    }

    public int getOffset() {
      return offset;
    }

    public int getByteLength() {
      return byteLength;
    }
  }

  public static class RevokedCertificate extends X509CRLEntry {

    private final BigInteger serialNumber;

    private final Date revocationDate;

    private final int reason;

    private final X500Name certificateIssuer;

    private RevokedCertificate(
            BigInteger serialNumber, Date revocationDate, int reason, X500Name certificateIssuer) {
      this.serialNumber = serialNumber;
      this.revocationDate = revocationDate;
      this.reason = reason;
      this.certificateIssuer = certificateIssuer;
    }

    @Override
    public byte[] getEncoded() throws CRLException {
      throw new UnsupportedOperationException("Getting the encoded bytes of the CRL entry is not supported.");
    }

    public BigInteger getSerialNumber() {
      return serialNumber;
    }

    public Date getRevocationDate() {
      return revocationDate;
    }

    @Override
    public boolean hasExtensions() {
      throw new UnsupportedOperationException("Checking extensions is not supported.");
    }

    @Override
    public String toString() {
      return serialNumber.toString();
    }

    public int getReason() {
      return reason;
    }

    public X500Principal getCertificateIssuer() {
      try {
          return new X500Principal(certificateIssuer.getEncoded());
      } catch (IOException e) {
        throw new RuntimeException("Unable to get the issuer for the CRL entry.", e);
      }
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
      throw new UnsupportedOperationException("Checking extensions is not supported.");
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
      throw new UnsupportedOperationException("Checking extensions is not supported.");
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
      throw new UnsupportedOperationException("Checking extensions is not supported.");
    }

    @Override
    public byte[] getExtensionValue(String oid) {
      throw new UnsupportedOperationException("Getting extension bytes is not supported.");
    }

    @Override
    public boolean equals(Object revokedCert) {
      return this.getClass().equals(revokedCert.getClass())
              && this.serialNumber.equals(((RevokedCertificate)revokedCert).getSerialNumber())
              && this.getCertificateIssuer().equals(((RevokedCertificate)revokedCert).getCertificateIssuer());
    }

    public int hashCode() {
      return Objects.hash(serialNumber, certificateIssuer);
    }
  }

  public class RevokedCertificateIterator implements Iterator<RevokedCertificate>, Closeable {

    private static final int UNSPECIFIED_CODE = 0;

    private BufferedInputStream inputStream;

    private RevokedCertificate next;

    private int offset;

    private RevokedCertificateIterator() throws IOException {
      this.inputStream = new BufferedInputStream(Files.newInputStream(crlFile.toPath()));
      skip(this.inputStream, firstRevokedCertificateOffset);
      this.offset = firstRevokedCertificateOffset;
      next0();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public RevokedCertificate next() {
      if (next == null) {
        throw new IllegalStateException("RevokedCertificateIterator does not have a next object.");
      }

      RevokedCertificate ret = next;
      next0();
      return ret;
    }

    private void next0() {
      if (offset >= revokedCertificatesEndIndex) {
        next = null;
        return;
      }

      byte[] bytes;
      try {
        bytes = readBlock(TAG_CONSTRUCTED_SEQUENCE, inputStream, "revokedCertificate");
      } catch (IOException ex) {
        throw new IllegalStateException("Error reading next revokedCertificate", ex);
      }
      offset += bytes.length;

      ASN1Sequence revokedCertificate = ASN1Sequence.getInstance(bytes);
      BigInteger serialNumber = ASN1Integer.getInstance(revokedCertificate.getObjectAt(0)).getValue();
      Date revocationDate = readTime(revokedCertificate.getObjectAt(1));
      int reason = UNSPECIFIED_CODE;
      X500Name certificateIssuer = null;

      if (revokedCertificate.size() > 2) {
        Extensions extensions = Extensions.getInstance(revokedCertificate.getObjectAt(2));
        byte[] coreExtValue = getCoreExtValue(extensions, Extension.certificateIssuer);
        if (coreExtValue != null) {
          certificateIssuer = X500Name.getInstance(GeneralNames.getInstance(coreExtValue).getNames()[0].getName());
        }

        coreExtValue = getCoreExtValue(extensions, Extension.reasonCode);
        if (coreExtValue != null) {
          reason = CRLReason.getInstance(coreExtValue).getValue().intValue();
        }
      }

      next = new RevokedCertificate(serialNumber, revocationDate, reason, certificateIssuer);
    }

    private byte[] getCoreExtValue(Extensions extensions, ASN1ObjectIdentifier extensionType) {
      if (extensions == null) {
        return null;
      }
      Extension extension = extensions.getExtension(extensionType);
      if (extension == null) {
        return null;
      }

      return extension.getExtnValue().getOctets();
    }

    @Override
    public void close() throws IOException {
      if (inputStream != null) {
        inputStream.close();
      }
      inputStream = null;
    }
  }
}
