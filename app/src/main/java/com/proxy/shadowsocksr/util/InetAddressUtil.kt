/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

//simple from org.apache.http.conn.util
package com.proxy.shadowsocksr.util

import java.util.regex.Pattern

class InetAddressUtil
{
    companion object
    {
        private val IPV4_BASIC_PATTERN_STRING = "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])"
        private val IPV4_PATTERN = Pattern.compile("^$IPV4_BASIC_PATTERN_STRING$")
        private val IPV6_STD_PATTERN = Pattern.compile(
                "^[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7}$")
        private val IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile(
                "^(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)::(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)$")

        private val COLON_CHAR = ':'
        private val MAX_COLON_COUNT = 7

        fun isIPv4Address(input: String): Boolean
        {
            return IPV4_PATTERN.matcher(input).matches()
        }

        fun isIPv6StdAddress(input: String): Boolean
        {
            return IPV6_STD_PATTERN.matcher(input).matches()
        }

        fun isIPv6HexCompressedAddress(input: String): Boolean
        {
            var colonCount = 0
            for (i in 0..input.length - 1)
            {
                if (input[i] == COLON_CHAR)
                {
                    colonCount++
                }
            }
            return colonCount <= MAX_COLON_COUNT && IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches()
        }

        fun isIPv6Address(input: String): Boolean
        {
            return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input)
        }
    }
}
