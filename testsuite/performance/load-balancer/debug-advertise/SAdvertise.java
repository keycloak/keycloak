/*
 *  Copyright(c) 2010 Red Hat Middleware, LLC,
 *  and individual contributors as indicated by the @authors tag.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library in the file COPYING.LIB;
 *  if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * @author Jean-Frederic Clere
 * @version $Revision: 420067 $, $Date: 2006-07-08 09:16:58 +0200 (sub, 08 srp 2006) $
 */
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class SAdvertise {

    /*
     * Server to test Advertize... Run it on httpd box to test for firewall.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: SAdvertize localaddress multicastaddress port");
            System.out.println("java SAdvertize 10.16.88.178 224.0.1.105 23364");
            System.out.println("send from 10.16.88.178:23364 to 224.0.1.105:23364");
            System.exit(1);
        }
        InetAddress group = InetAddress.getByName(args[1]);
        InetAddress addr = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress addrs = new InetSocketAddress(addr, port);

        MulticastSocket s = new MulticastSocket(addrs);
        s.setTimeToLive(29);
        s.joinGroup(group);
        boolean ok = true;
        while (ok) {
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length, group, port);
            System.out.println("sending from: " + addr);
            s.send(recv);
            Thread.currentThread().sleep(2000);
        }
        s.leaveGroup(group);
    }
}
