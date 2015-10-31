package com.proxy.shadowsocksr.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Change by Kagayama Kaede.
 */

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressUtils
{
    private static final long UNSIGNED_INT_MASK = 0x0FFFFFFFFL;

    private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
    private static final Pattern addressPattern = Pattern.compile(IP_ADDRESS);
    private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);

    public static String ipv4BytesToIp(byte[] bytes)
    {
        return new StringBuilder()
                .append(bytes[0] & 0xFF).append('.')
                .append(bytes[1] & 0xFF).append('.')
                .append(bytes[2] & 0xFF).append('.')
                .append(bytes[3] & 0xFF).toString();
    }

    public static boolean checkInCIDRRange(String ip, List<String> cidrs)
    {
        Matcher matcher;
        for (String cidr : cidrs)
        {
            int netmask = 0;
            int address = 0;
            int network;
            int broadcast;
            //
            matcher = cidrPattern.matcher(cidr);
            if (matcher.matches())
            {
                for (int i = 1; i <= 4; ++i)
                {
                    int n = Integer.parseInt(matcher.group(i));
                    address |= ((n & 0xff) << 8 * (4 - i));
                }

                int cidrPart = Integer.parseInt(matcher.group(5));
                for (int j = 0; j < cidrPart; ++j)
                {
                    netmask |= (1 << 31 - j);
                }

                network = (address & netmask);
                broadcast = network | ~(netmask);

                int ipaddr = 0;
                matcher = addressPattern.matcher(ip);
                if (matcher.matches())
                {
                    for (int i = 1; i <= 4; ++i)
                    {
                        int n = Integer.parseInt(matcher.group(i));
                        ipaddr |= ((n & 0xff) << 8 * (4 - i));
                    }
                    //
                    long addLong = ipaddr & UNSIGNED_INT_MASK;
                    long lowLong =
                            ((broadcast & UNSIGNED_INT_MASK) - (network & UNSIGNED_INT_MASK) > 1 ?
                             network + 1 : 0) & UNSIGNED_INT_MASK;
                    long highLong =
                            ((broadcast & UNSIGNED_INT_MASK) - (network & UNSIGNED_INT_MASK) > 1 ?
                             broadcast - 1 : 0) & UNSIGNED_INT_MASK;
                    if (addLong >= lowLong && addLong <= highLong)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
