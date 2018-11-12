package impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public abstract class AbstractFtp {

    String username;
    String password;
    String host;
    int port;
    String localpath;
    String remotepath;
    String order;      //增量下载[fname:文件名;mtime:修改时间];

    BreakPoint breakPoint;


    AbstractFtp(String username, String password, String host,
                int port, String localpath, String remotepath) throws IOException {
        this(username, password, host, port, localpath, remotepath, "", "");
    }

    /**
     * servieId:记录断点文件
     */
    AbstractFtp(String username, String password, String host, int port, String localpath, String remotepath,
                String order, String servieId) throws IOException {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.localpath = localpath;
        this.remotepath = remotepath;
        this.order = order;
        if (!servieId.isEmpty()) this.breakPoint = new BreakPoint(servieId);
    }

    //存在三种模式OVERWRITE,RESUME,APPEND;默认OVERWRITE
    public void upload() throws Exception {
        rmkdir(remotepath, 0);
        upload_r(localpath);
    }

    public abstract void download() throws Exception;

    public abstract void login() throws Exception;

    public abstract void logout() throws Exception;


    private void upload_r(String path) throws Exception {
        File pf = Paths.get(path).toFile();
        if (pf.isFile()) _upload(path, "");
        else {
            File[] files = pf.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String _rel = file.getParent().substring(localpath.length());
                        _upload(file.getPath(), _rel);
                    } else upload_r(file.getPath());
                }
            }
        }
    }

    abstract void rmkdir(String dir, int start) throws Exception;

    abstract void _upload(String srcFile, String relative) throws Exception;

}
