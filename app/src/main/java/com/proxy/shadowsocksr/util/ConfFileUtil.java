package com.proxy.shadowsocksr.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ConfFileUtil
{
    public static String SSRConf =
            "{\"server\": \"%s\"," +
            " \"server_port\": %d," +
            " \"local_port\": %d," +
            " \"password\": \"%s\"," +
            " \"method\":\"%s\"," +
            " \"timeout\": %d}";

    public static String RedSocksConf =
            "base {" +
            " log_debug = off;" +
            " log_info = off;" +
            " log = stderr;" +
            " daemon = on;" +
            " redirector = iptables;" +
            "}" +
            "redsocks {" +
            " local_ip = 127.0.0.1;" +
            " local_port = 8123;" +
            " ip = 127.0.0.1;" +
            " port = %d;" +
            " type = socks5;" +
            "}";

    //TODO must split line?
    public static String lineSept = System.getProperty("line.separator");
    public static String PDNSdLocal =
            "global {" + lineSept +
            " perm_cache = 2048;" + lineSept +
            " cache_dir = \"/data/data/github.shadowsocksrdroid\";" + lineSept +
            " server_ip = %s;" + lineSept +
            " server_port = %d;" + lineSept +
            " query_method = tcp_only;" + lineSept +
            " run_ipv4 = on;" + lineSept +
            " min_ttl = 15m;" + lineSept +
            " max_ttl = 1w;" + lineSept +
            " timeout = 10;" + lineSept +
            " daemon = on;" + lineSept +
            " pid_file = %s;" + lineSept +
            "}" + lineSept +
            "server {" + lineSept +
            " label = \"local\";" + lineSept +
            " ip = 127.0.0.1;" + lineSept +
            " port = %d;" + lineSept +
            " reject = ::/0;" + lineSept +
            " reject_policy = negate;" + lineSept +
            " reject_recursively = on;" + lineSept +
            " timeout = 5;" + lineSept +
            "}" + lineSept +
            "rr {" + lineSept +
            " name=localhost;" + lineSept +
            " reverse=on;" + lineSept +
            " a=127.0.0.1;" + lineSept +
            " owner=localhost;" + lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;"+ lineSept +
            "}";

    public static String PdNSdByPass =
            "global {" + lineSept +
            " perm_cache = 2048;" + lineSept +
            " cache_dir = \"/data/data/github.shadowsocksrdroid\";" + lineSept +
            " server_ip = %s;" + lineSept +
            " server_port = %d;" + lineSept +
            " query_method = tcp_only;" + lineSept +
            " run_ipv4 = on;" + lineSept +
            " min_ttl = 15m;" + lineSept +
            " max_ttl = 1w;" + lineSept +
            " timeout = 10;" + lineSept +
            " daemon = on;" + lineSept +
            " pid_file = %s;" + lineSept +
            "}" + lineSept +
            "server {" + lineSept +
            " label = \"china-servers\";" + lineSept +
            " ip = 114.114.114.114, 223.5.5.5;" + lineSept +
            " uptest = none;" + lineSept +
            " preset = on;" + lineSept +
            " include = %s;" + lineSept +
            " policy = excluded;" + lineSept +
            " timeout = 2;" + lineSept +
            "}" + lineSept +
            "server {" + lineSept +
            " label = \"local-server\";" + lineSept +
            " ip = 127.0.0.1;" + lineSept +
            " uptest = none;" + lineSept +
            " preset = on;" + lineSept +
            " timeout = 5;" + lineSept +
            "}" + lineSept +
            "rr {" + lineSept +
            " name=localhost;" + lineSept +
            " reverse=on;" + lineSept +
            " a=127.0.0.1;" + lineSept +
            " owner=localhost;" + lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;" + lineSept +
            "}";

    public static String PdNSdDirect =
            "global {" + lineSept +
            " perm_cache = 2048;" + lineSept +
            " cache_dir = \"/data/data/github.shadowsocksrdroid\";" + lineSept +
            " server_ip = %s;" + lineSept +
            " server_port = %d;" + lineSept +
            " query_method = tcp_only;" + lineSept +
            " run_ipv4 = on;" + lineSept +
            " min_ttl = 15m;" + lineSept +
            " max_ttl = 1w;" + lineSept +
            " timeout = 10;" + lineSept +
            " daemon = on;" + lineSept +
            " pid_file = %s;" + lineSept +
            "}" + lineSept +
            "server {" + lineSept +
            " label = \"china-servers\";" + lineSept +
            " ip = 114.114.114.114, 112.124.47.27;" + lineSept +
            " timeout = 4;" + lineSept +
            " reject = %s;" + lineSept + lineSept +
            " reject_policy = fail;" + lineSept +
            " reject_recursively = on;" + lineSept +
            " exclude = %s;" + lineSept +
            " policy = included;" + lineSept +
            " preset = on;" + lineSept +
            "}" + lineSept +
            "server {" + lineSept +
            " label = \"local-server\";" + lineSept +
            " ip = 127.0.0.1;" + lineSept +
            " port = %d;" + lineSept +
            " reject = ::/0;" + lineSept +
            " reject_policy = negate;" + lineSept +
            " reject_recursively = on;" + lineSept +
            "}" + lineSept +
            "rr {" + lineSept +
            " name=localhost;" + lineSept +
            " reverse=on;" + lineSept +
            " a=127.0.0.1;" + lineSept +
            " owner=localhost;" + lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;"+ lineSept +
            "}";

    public static void writeToFile(String c, File f)
    {
        try
        {
            PrintWriter pw = new PrintWriter(f);
            pw.write(c);
            pw.flush();
            pw.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}
