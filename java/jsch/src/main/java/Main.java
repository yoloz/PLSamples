import impl.AbstractFtp;
import impl.Sftp;


public class Main {

    private final static String helper = "jsch.sh [params...]" + "\n" +
            "protocol[sftp,ftp]" + "\n" +
            "username" + "\n" +
            "password" + "\n" +
            "host" + "\n" +
            "port" + "\n" +
            "localpath" + "\n" +
            "remotepath" + "\n" +
            "action[down,up]" + "\n" +
            "order[fname,mtime]:非必要参数,增量下载" + "\n" +
            "serviceId:非必要参数,增量下载";

    /**
     * protocol[sftp,ftp]:ftp暂未实现
     * username
     * password
     * host
     * port
     * localpath
     * remotepath
     * action[down,up]
     * order[fname,mtime]
     * serviceId
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length >= 8) {
            AbstractFtp abstractFtp;
            if ("sftp".equals(args[0])) {
                if (args.length > 8)
                    abstractFtp = new Sftp(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], args[6],
                            args[8], args[9]);
                else abstractFtp = new Sftp(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], args[6]);
            } else throw new Exception("协议[" + args[0] + "]未实现...");
            abstractFtp.login();
            try {
                if ("down".equals(args[7])) abstractFtp.download();
                else if ("up".equals(args[7])) abstractFtp.upload();
                else throw new Exception("操作[" + args[7] + "]未实现...");
            } finally {
                abstractFtp.logout();
            }
        } else throw new Exception("参数为空或者参数过少\n" + helper);

    }
}
