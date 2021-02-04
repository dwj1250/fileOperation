import java.io.File;

public class FileOperation {

    private static void listFiles() {
        // File[] listFile()：返回一个File数组
        File file = new File("H:/LZF");
        File[] files = file.listFiles();
        for (File f:files) {
            System.out.println(f.getName());
        }
        String[] strings= file.list();
        for (String string : strings) {
            System.out.println(string);
        }

    }

    public static void main(String[] args) {
        listFiles();
    }

}
