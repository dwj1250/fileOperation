import ch.ethz.ssh2.*;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class Test {


    /**
     *
     * @param dataServerIp 服务器IP
     * @param dataServerUsername 服务器用户名
     * @param dataServerPassword 服务器登录密码
     * @param srcFile 要下载的文件路径
     * @param saveFile 保存路径
     * @param port 端口号，null时为默认端口
     */
    public static void  downLoadFile(String dataServerIp,String dataServerUsername,String dataServerPassword,String srcFile,String saveFile,int port){
        Connection conn = new Connection(dataServerIp);
        Session session = null;
        SCPClient client =null;
        //session=(Session) getObject(dataServerIp, dataServerUsername, dataServerPassword, 0, "session");
        //client=(SCPClient) getObject(dataServerIp, dataServerUsername, dataServerPassword, 0, "client");
        conn=getConn(dataServerIp, dataServerUsername, dataServerPassword, 22);
        try {
            session=conn.openSession();
            client=conn.createSCPClient();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            boolean flag=download(srcFile, saveFile, session, client);
            //如果是打包文件，删除包裹
            if (flag) {
                System.out.println("文件打包下载完成！");
                //解压tar.gz包
                String fileName=srcFile.substring(srcFile.lastIndexOf("/")+1);
                File file=new File(saveFile+"/"+fileName+".tar.gz");
                unTarGz(file, saveFile);
                System.out.println("文件解压完成！");
                //解压完后删除本地压缩包
                file.delete();
                String cmdDel="rm -rf "+srcFile+".tar.gz";
                session=(Session) getObject(dataServerIp, dataServerUsername, dataServerPassword, 0, "session");
                //删除服务器生成的压缩包
                session.execCommand(cmdDel);
            }else {
                System.out.println("文件下载完成！");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            session.close();
            conn.close();
        }
    }
    /**
     * 根据不同的需求得到相应的连接
     * @param ip
     * @param userName
     * @param pwd
     * @param port
     * @param whatWant 必须是client或者session
     * @return
     */
    public static Object getObject(String ip,String userName,String pwd,int port,String whatWant){
        Connection conn = new Connection(ip);
        Session session = null;
        SCPClient client =null;
        try {
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(userName, pwd);
            session=conn.openSession();
            client = new SCPClient(conn);
            if (isAuthenticated == false) {
                throw new IOException("Authentication failed.文件scp到数据服务器时发生异常");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (whatWant!=null&&whatWant.equals("session")) {
            return session;
        }if (whatWant!=null&&whatWant.equals("client")) {
            return client;
        }
        return null;
    }
    /**
     * 获得连接
     * @param ip
     * @param userName
     * @param pwd
     * @param port
     * @return
     */
    public static Connection getConn(String ip,String userName,String pwd,int port){
        Connection conn = new Connection(ip);
        boolean blag=false;
        try {
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(userName, pwd);
            //session=conn.openSession();
            // client = new SCPClient(conn);
            if (isAuthenticated) {
                blag=true;
            }
            if (isAuthenticated == false) {
                throw new IOException("Authentication failed.文件scp到数据服务器时发生异常");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (blag) {
            return conn;
        }else {
            return null;
        }
    }
    /**
     *
     * @param srcFile 要下载的文件
     * @param saveFile 保存目录，必须是目录
     * @param sessionSsh
     * @param client
     * @return
     * @throws UnsupportedEncodingException
     */
    public static boolean download(String srcFile, String saveFile, Session sessionSsh, SCPClient client
    ) throws UnsupportedEncodingException {
        //String cdmTar="tar czf /var/ftp/upload/ruku/ruku.tar.gz –directory=/var/ftp/upload/ruku ruku";
        boolean flag=false;
        // 是文件,直接下载
        SCPInputStream inputStream=null;
        FileOutputStream fos= null;
        try {
            fos = new FileOutputStream(saveFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            String filename=srcFile.substring(srcFile.lastIndexOf("/")+1);
            if (filename.contains(".")) {
                try {
                    inputStream=client.get(srcFile);
                } catch (Exception e) {
                    //是文件夹，打包下载
                    String src=srcFile.substring(0, srcFile.lastIndexOf("/"));
                    String cmdGet = "tar -zcvf " + srcFile + ".tar.gz " + filename;
                    //String cmdGet="tar czf /home/"+filename+".tar.gz –directory="+src+"/"+filename+".";
                    // 执行压缩命令
                    sessionSsh.execCommand("cd "+src+";"+cmdGet);
                    InputStream stdout = new StreamGobbler(sessionSsh.getStdout());
                    BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                    // cmriots!@#
                    while (true) {
                        String line = br.readLine();
                        if (line == null)
                            break;
                        System.out.println(line);
                    }
                    System.out.println("ExitCode: " + sessionSsh.getExitStatus());
                    // 下载文件
                    inputStream=client.get(src+"/"+filename+".tar.gz");
                    flag=true;
                    e.printStackTrace();
                }
            }else {
                //是文件夹，打包下载
                String src=srcFile.substring(0, srcFile.lastIndexOf("/"));
                String cmdGet = "tar -zcvf " + srcFile + ".tar.gz " + filename;
                //String cmdGet="tar czf /home/"+filename+".tar.gz –directory="+src+"/"+filename+".";
                // 执行压缩命令
                sessionSsh.execCommand("cd "+src+";"+cmdGet);
                InputStream stdout = new StreamGobbler(sessionSsh.getStdout());
                BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                // cmriots!@#
                while (true) {
                    String line = br.readLine();
                    if (line == null)
                        break;
                    System.out.println(line);
                }
                System.out.println("ExitCode: " + sessionSsh.getExitStatus());
                // 下载文件
                inputStream=client.get(src+"/"+filename+".tar.gz");
                //unGzipFile(saveFile+"/"+filename+".tar.gz");
                flag=true;
            }
            File f = new File(saveFile);
            if(!f.exists()){
                f.mkdirs();
            }
            byte[] b = new byte[4096];
            int i;
            while ((i = inputStream.read(b)) != -1){
                fos.write(b,0, i);
            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    //------------------------------------------------------------------------------------------------------
    /**
     * 解压tar.gz 文件
     * @param file 要解压的tar.gz文件对象
     * @param outputDir 要解压到某个指定的目录下
     * @throws IOException
     */
    public static void unTarGz(File file,String outputDir) throws IOException{
        TarInputStream tarIn = null;
        try{
            tarIn = new TarInputStream(new GZIPInputStream(
                    new BufferedInputStream(new FileInputStream(file))),
                    1024 * 2);

            createDirectory(outputDir,null);//创建输出目录

            TarEntry entry = null;
            while( (entry = tarIn.getNextEntry()) != null ){

                if(entry.isDirectory()){//是目录
                    entry.getName();
                    createDirectory(outputDir,entry.getName());//创建空目录
                }else{//是文件
                    File tmpFile = new File(outputDir + "/" + entry.getName());
                    createDirectory(tmpFile.getParent() + "/",null);//创建输出目录
                    OutputStream out = null;
                    try{
                        out = new FileOutputStream(tmpFile);
                        int length = 0;

                        byte[] b = new byte[2048];

                        while((length = tarIn.read(b)) != -1){
                            out.write(b, 0, length);
                        }

                    }catch(IOException ex){
                        throw ex;
                    }finally{

                        if(out!=null)
                            out.close();
                    }
                }
            }
        }catch(IOException ex){
            throw new IOException("解压归档文件出现异常");
        } finally{
            try{
                if(tarIn != null){
                    tarIn.close();
                }
            }catch(IOException ex){
                throw new IOException("关闭tarFile出现异常");
            }
        }
    }
    /**
     * 构建目录
     * @param outputDir
     * @param subDir
     */
    public static void createDirectory(String outputDir,String subDir){
        File file = new File(outputDir);
        if(!(subDir == null || subDir.trim().equals(""))){//子目录不为空
            file = new File(outputDir + "/" + subDir);
        }
        if(!file.exists()){
            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            file.mkdirs();
        }
    }

}
