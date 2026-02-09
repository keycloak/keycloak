/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jose.jwk;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Test class for <em>JWKUtil.toIntegerBytes</em> methods.</p>
 *
 * @author rmartinc
 */
public class JWKUtilTest {

    @Test
    public void testBigInteger256bit33bytes() {
        // big integer that is 256b/32B (P-256) but positive sign adds one more byte
        BigInteger bi = new BigInteger("106978455244904118504029146852168092303170743300495577837424194202315290288011");
        Assert.assertEquals(256, bi.bitLength());
        Assert.assertEquals(33, bi.toByteArray().length);
        byte[] bytes = JWKUtil.toIntegerBytes(bi, 256);
        Assert.assertEquals(32, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
    }

    @Test
    public void testBigInteger521bit66bytes() {
        // big integer that is 521b/66B (P-521)
        BigInteger bi = new BigInteger("6734373674814691396115132088653791161514881890352734019594374673014557152383502505390504647094584246525242385854438954847939940255492102589858760446395824148");
        Assert.assertEquals(521, bi.bitLength());
        Assert.assertEquals(66, bi.toByteArray().length);
        byte[] bytes = JWKUtil.toIntegerBytes(bi, 521);
        Assert.assertEquals(66, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
    }

    @Test
    public void testBigInteger519bit65bytes() {
        // big integer is 519b/65B (P-521)
        BigInteger bi = new BigInteger("1056406612537758216307284361941630998827278875643943164504783316640832530092186610655845467862847840003942818620330993843247554843391332954698064457598103921");
        Assert.assertEquals(519, bi.bitLength());
        Assert.assertEquals(65, bi.toByteArray().length);
        byte[] bytes = JWKUtil.toIntegerBytes(bi, 521);
        Assert.assertEquals(66, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
        bytes = JWKUtil.toIntegerBytes(bi);
        Assert.assertEquals(65, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
    }

    @Test
    public void testBigInteger509bit65bytes() {
        // big integer is 509b/64B (P-521)
        BigInteger bi = new BigInteger("1020105336060806799317581876370378670178920448263046037385822665297838480884942245045412789346716977404456327079571798657084244307627713218035021026706753");
        Assert.assertEquals(509, bi.bitLength());
        Assert.assertEquals(64, bi.toByteArray().length);
        byte[] bytes = JWKUtil.toIntegerBytes(bi, 521);
        Assert.assertEquals(66, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
        bytes = JWKUtil.toIntegerBytes(bi);
        Assert.assertEquals(64, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
    }

    @Test
    public void testBigInteger380bit48bytes() {
        // big integer is 380b/48B (P-384)
        BigInteger bi = new BigInteger("1318324198847573133767761135109898830134893480775680898178696604234765693579204018161102886445531980641666395659568");
        Assert.assertEquals(380, bi.bitLength());
        Assert.assertEquals(48, bi.toByteArray().length);
        byte[] bytes = JWKUtil.toIntegerBytes(bi, 384);
        Assert.assertEquals(48, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
        bytes = JWKUtil.toIntegerBytes(bi);
        Assert.assertEquals(48, bytes.length);
        Assert.assertEquals(bi, new BigInteger(1, bytes));
    }

    @Test
    public void testBigInteger380bit48bytesErrorFor256() {
        // big integer is 380b/48B (P-384) not valid for 256b (P-256)
        BigInteger bi = new BigInteger("1318324198847573133767761135109898830134893480775680898178696604234765693579204018161102886445531980641666395659568");
        Assert.assertEquals(380, bi.bitLength());
        Assert.assertEquals(48, bi.toByteArray().length);
        AssertionError e = Assert.assertThrows(AssertionError.class, () -> JWKUtil.toIntegerBytes(bi, 256));
        Assert.assertEquals("Incorrect big integer with bit length 380 for 256", e.getMessage());
    }
}
