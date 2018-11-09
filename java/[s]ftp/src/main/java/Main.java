import impl.AbstractFtp;
import impl.Sftp;

public class Main {

    /**
     * protocol[sftp,ftp]:ftp暂未实现
     * username
     * password
     * host
     * port
     * localpath
     * remotepath
     * action[down,up]
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 8) {
            AbstractFtp abstractFtp;
            if ("sftp".equals(args[0]))
                abstractFtp = new Sftp(args[1], args[2], args[3], Integer.parseInt(args[4]), args[5], args[6]);
            else throw new Exception("协议[" + args[0] + "]未实现...");
            abstractFtp.login();
            try {
                if ("down".equals(args[7])) abstractFtp.download();
                else if ("up".equals(args[7])) abstractFtp.upload();
                else throw new Exception("操作[" + args[7] + "]未实现...");
            } finally {
                abstractFtp.logout();
            }
        } else throw new Exception("参数为空或者参数过少");

    }
}
