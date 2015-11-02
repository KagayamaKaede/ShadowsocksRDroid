package com.proxy.shadowsocksr.util;

import com.proxy.shadowsocksr.Consts;

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
    public static String PdNSdLocal =
            "global {" + Consts.lineSept +
            " perm_cache = 2048;" + Consts.lineSept +
            " cache_dir = \"/data/data/com.proxy.shadowsocksr\";" + Consts.lineSept +
            " server_ip = %s;" + Consts.lineSept +
            " server_port = %d;" + Consts.lineSept +
            " query_method = udp_only;" + Consts.lineSept +
            " run_ipv4 = on;" + Consts.lineSept +
            " min_ttl = 15m;" + Consts.lineSept +
            " max_ttl = 1w;" + Consts.lineSept +
            " timeout = 10;" + Consts.lineSept +
            " daemon = on;" + Consts.lineSept +
            " pid_file = %s;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "server {" + Consts.lineSept +
            " label = \"local\";" + Consts.lineSept +
            " ip = 127.0.0.1;" + Consts.lineSept +
            " port = %d;" + Consts.lineSept +
            " %s" + Consts.lineSept +
            " reject_policy = negate;" + Consts.lineSept +
            " reject_recursively = on;" + Consts.lineSept +
            " timeout = 5;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "rr {" + Consts.lineSept +
            " name=localhost;" + Consts.lineSept +
            " reverse=on;" + Consts.lineSept +
            " a=127.0.0.1;" + Consts.lineSept +
            " owner=localhost;" + Consts.lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;" + Consts.lineSept +
            "}";

    public static String PdNSdByPass =
            "global {" + Consts.lineSept +
            " perm_cache = 2048;" + Consts.lineSept +
            " cache_dir = \"/data/data/com.proxy.shadowsocksr\";" + Consts.lineSept +
            " server_ip = %s;" + Consts.lineSept +
            " server_port = %d;" + Consts.lineSept +
            " query_method = udp_only;" + Consts.lineSept +
            " run_ipv4 = on;" + Consts.lineSept +
            " min_ttl = 15m;" + Consts.lineSept +
            " max_ttl = 1w;" + Consts.lineSept +
            " timeout = 10;" + Consts.lineSept +
            " daemon = on;" + Consts.lineSept +
            " pid_file = %s;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "server {" + Consts.lineSept +
            " label = \"china-servers\";" + Consts.lineSept +
            " ip = 114.114.114.114, 223.5.5.5;" + Consts.lineSept +
            " uptest = none;" + Consts.lineSept +
            " preset = on;" + Consts.lineSept +
            " include = %s;" + Consts.lineSept +
            " policy = excluded;" + Consts.lineSept +
            " timeout = 2;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "server {" + Consts.lineSept +
            " label = \"local-server\";" + Consts.lineSept +
            " ip = 127.0.0.1;" + Consts.lineSept +
            " uptest = none;" + Consts.lineSept +
            " preset = on;" + Consts.lineSept +
            " port = %d;" + Consts.lineSept +
            " timeout = 5;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "rr {" + Consts.lineSept +
            " name=localhost;" + Consts.lineSept +
            " reverse=on;" + Consts.lineSept +
            " a=127.0.0.1;" + Consts.lineSept +
            " owner=localhost;" + Consts.lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;" + Consts.lineSept +
            "}";

    public static String PdNSdDirect =
            "global {" + Consts.lineSept +
            " perm_cache = 2048;" + Consts.lineSept +
            " cache_dir = \"/data/data/com.proxy.shadowsocksr\";" + Consts.lineSept +
            " server_ip = %s;" + Consts.lineSept +
            " server_port = %d;" + Consts.lineSept +
            " query_method = udp_only;" + Consts.lineSept +
            " run_ipv4 = on;" + Consts.lineSept +
            " min_ttl = 15m;" + Consts.lineSept +
            " max_ttl = 1w;" + Consts.lineSept +
            " timeout = 10;" + Consts.lineSept +
            " daemon = on;" + Consts.lineSept +
            " pid_file = %s;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "server {" + Consts.lineSept +
            " label = \"china-servers\";" + Consts.lineSept +
            " ip = 114.114.114.114, 112.124.47.27;" + Consts.lineSept +
            " timeout = 4;" + Consts.lineSept +
            " reject = %s;" + Consts.lineSept +
            " reject_policy = fail;" + Consts.lineSept +
            " reject_recursively = on;" + Consts.lineSept +
            " exclude = %s;" + Consts.lineSept +
            " policy = included;" + Consts.lineSept +
            " uptest = none;" + Consts.lineSept +
            " preset = on;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "server {" + Consts.lineSept +
            " label = \"local-server\";" + Consts.lineSept +
            " ip = 127.0.0.1;" + Consts.lineSept +
            " port = %d;" + Consts.lineSept +
            " %s" + Consts.lineSept +
            " reject_policy = negate;" + Consts.lineSept +
            " reject_recursively = on;" + Consts.lineSept +
            "}" + Consts.lineSept +
            "rr {" + Consts.lineSept +
            " name=localhost;" + Consts.lineSept +
            " reverse=on;" + Consts.lineSept +
            " a=127.0.0.1;" + Consts.lineSept +
            " owner=localhost;" + Consts.lineSept +
            " soa=localhost,root.localhost,42,86400,900,86400,86400;" + Consts.lineSept +
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
