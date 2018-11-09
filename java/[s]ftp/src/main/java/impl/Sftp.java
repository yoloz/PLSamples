package impl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Vector;

public class Sftp extends AbstractFtp {


    private ChannelSftp sftp;
    private Session session;

    public Sftp(String username, String password, String host, int port,
                String localpath, String remotepath) {
        super(username, password, host, port, localpath, remotepath);
    }

    @Override
    public void login() throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftp = (ChannelSftp) channel;
    }

    @Override
    public void logout() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Override
    void rmkdir(String dir, int start) throws Exception {
        Path path = Paths.get(dir);
        int count = path.getNameCount();
        StringBuilder s = new StringBuilder("/");
        if (start > 0) {
            for (int i = 0; i < start; i++) {
                s.append(path.getName(i)).append("/");
            }
        }
        for (int i = start; i < count; i++) {
            s.append(path.getName(i));
            try {
                sftp.cd(s.toString());
            } catch (SftpException e) {
                sftp.mkdir(s.toString());
            }
            if (i != count - 1) s.append("/");
        }
    }


    @Override
    void _upload(String srcFile, String relative) throws Exception {
        Path remoteP = Paths.get(remotepath);
        Path targetPath = remoteP;
        if (!relative.isEmpty()) {
            if (relative.startsWith("/")) relative = relative.substring(1);
            targetPath = remoteP.resolve(relative);
            rmkdir(targetPath.toString(), remoteP.getNameCount());
        }
        sftp.put(srcFile, targetPath.toString());
    }

    @Override
    public void download() throws Exception {
        Files.createDirectories(Paths.get(localpath));
        download_r(remotepath);
    }

    private void download_r(String path) throws Exception {
        Vector lsEntries = sftp.ls(path);
        for (Object lsEntry : lsEntries) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) lsEntry;
            String fname = entry.getFilename();
            if (".".equals(fname) || "..".equals(fname)) continue;
            if (entry.getAttrs().isDir()) {
                download_r(Paths.get(path, fname).toString());
            } else if (entry.getAttrs().isReg()) {
                if (path.endsWith(fname)) _download(path);
                else _download(Paths.get(path, fname).toString());
            }
        }
    }

    private void _download(String file) throws Exception {
        Path path = Paths.get(file);
        String relative = "";
        if (!file.equals(remotepath)) {
            if (remotepath.endsWith("/")) relative = path.getParent().toString().substring(remotepath.length() - 1);
            else relative = path.getParent().toString().substring(remotepath.length());
        }
        Path targetPath = Paths.get(localpath);
        if (!relative.isEmpty()) {
            if (relative.startsWith("/")) relative = relative.substring(1);
            targetPath = Paths.get(localpath).resolve(relative);
            Files.createDirectories(Paths.get(localpath).resolve(relative));
        }
        sftp.get(file, targetPath.resolve(path.getFileName()).toString());
    }


    public static void main(String[] args) {
//        impl.Sftp sftp = new impl.Sftp("ylz", "ylzhang", "10.68.120.111", 22,
//                "/home/ylzhang/test", "/home/ylz/test4/");
        Sftp sftp = new Sftp("ylz", "ylzhang", "10.68.120.111", 22,
                "/home/ylzhang/test2", "/home/ylz/test4/");
        try {
            sftp.login();
//            sftp.upload();
            sftp.download();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sftp.logout();
        }

    }

}
