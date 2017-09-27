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
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;

public class Advertise
{
    /*
     * Client to test Advertize... Run it on node boxes to test for firewall.
     */
    public static void main(String[] args) throws Exception
    {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: Advertize multicastaddress port [bindaddress]");
            System.out.println("java Advertize 224.0.1.105 23364");
            System.out.println("or");
            System.out.println("java Advertize 224.0.1.105 23364 10.33.144.3");
            System.out.println("receive from 224.0.1.105:23364");
            System.exit(1);
        }
 
        InetAddress group = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        InetAddress socketInterface = null;
        if (args.length == 3)
            socketInterface = InetAddress.getByName(args[2]);
        MulticastSocket s = null;
        String value = System.getProperty("os.name");
        if ((value != null) && (value.toLowerCase().startsWith("linux") || value.toLowerCase().startsWith("mac") || value.toLowerCase().startsWith("hp"))) {
           System.out.println("Linux like OS");
           s = new MulticastSocket(new InetSocketAddress(group, port));
        } else
           s = new MulticastSocket(port);
        s.setTimeToLive(0);
        if (socketInterface != null) {
            s.setInterface(socketInterface);
        }
        s.joinGroup(group);
        boolean ok = true;
        System.out.println("ready waiting...");
        while (ok) {
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            String data = new String(buf);
            System.out.println("received: " + data);
            System.out.println("received from " + recv.getSocketAddress());
        }
        s.leaveGroup(group);
    }
}
