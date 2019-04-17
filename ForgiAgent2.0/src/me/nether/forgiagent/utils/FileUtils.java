package me.nether.forgiagent.utils;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * File Utils
 *
 * @author NetherSoul
 * @credits some utils from the old MC launcher
 */
public class FileUtils {

    /**
     * Copies a file from a source to a destination
     *
     * @param source File to be copied
     * @param dest   File where the source will be copied
     * @throws IOException
     */
    public static void copyFile(File source, File dest) throws IOException {
        if (source.getPath().equals(dest.getPath())) return;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            is.close();
            os.close();
        }
    }

    /**
     * Used to get the executing jar file directory
     *
     * @return current jar directory
     */
    public static File getCurrentJarPath() {
        try {
            return new File(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the size of a folder's file recursively
     * <p>
     * DOESN'T count the folder size itself, only the files inside
     *
     * @param path
     * @return
     */
    public static long getDirSize(File path) {
        long size = 0;

        if (path.listFiles() == null) {
            return size;
        }

        for (File file : path.listFiles()) {
            if (file.isDirectory()) {
                size += getDirSize(file);
            } else {
                size += file.length();
            }
        }

        return size;
    }

    /**
     * Recurively lists the file in a Directory, without the directories themselves
     *
     * @param path of the folder to check
     * @return List of files inside the folder, and the files inside the folders of the path (etc... etc...)
     */
    public static List<File> listFiles(File path) {
        List<File> fileList = new ArrayList<File>();

        if (!path.isDirectory()) {
            fileList.add(path);
            return fileList;
        } else {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    fileList.add(file);
                } else {
                    fileList.addAll(listFiles(file));
                }
            }
        }

        return fileList;
    }

    /**
     * Similar to listFiles, but with the option to keep folders in the list
     * <p>
     * TODO: unify all 3 methods
     *
     * @param path
     * @param keepDirectories
     * @return List of files in the folder ...
     */
    public static List<File> getAllFilesInFolder(File path, boolean keepDirectories) {
        List<File> fileList = new ArrayList<File>();

        if (!path.isDirectory()) {
            fileList.add(path);
            return fileList;
        } else {
            for (File file : path.listFiles()) {
                if (!file.isDirectory() || keepDirectories) {
                    fileList.add(file);
                }
            }
        }

        return fileList;
    }

    /**
     * Same as above, but with exclude pattern
     * <p>
     * TODO: Unify
     *
     * @param path
     * @param keepDirectories
     * @param exclude
     * @return List of files in the folder ...
     */
    public static List<File> getAllFilesInFolder(File path, boolean keepDirectories, String... exclude) {
        List<File> fileList = new ArrayList<File>();

        if (!path.isDirectory()) {
            fileList.add(path);
            return fileList;
        }

        for (File file : path.listFiles()) {
            boolean toExclude = false;
            for (String s : exclude) {
                if (file.getAbsolutePath().contains(s)) {
                    toExclude = true;
                    break;
                }
            }
            if (!toExclude || !keepDirectories)
                fileList.addAll(getAllFilesInFolder(file, keepDirectories, exclude));
        }

        return fileList;
    }

    /**
     * Turns the lines of a file into a List of strings
     *
     * @param file to read
     * @return List of strings from the File
     * @throws Exception
     */
    public static List<String> readFileLines(File file) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    /**
     * Turns the response of a basic http request to a List of strings
     * <p>
     * Used only on single line response type, with a separator pattern
     *
     * @param url       to connect to
     * @param separator split parameter
     * @return List of strings from the response
     */
    public static List<String> readUrlLineResponseSplitted(URL url, String separator) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        for (String s : reader.readLine().split(separator)) {
            lines.add(s);
        }
        reader.close();
        return lines;
    }

    /**
     * Turns the response's lines of a basic http request to a List of strings
     *
     * @param url
     * @return List of strings from the response
     * @throws Exception
     */
    public static List<String> readUrlResponse(URL url) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (!inputLine.isEmpty()) {
                lines.add(inputLine);
            }
        }
        in.close();
        return lines;
    }

    /**
     * Prints a File from a List of strings
     *
     * @param dir  where to print
     * @param list what to print
     * @throws Exception
     */
    public static void printFile(File dir, List<String> list) throws Exception {
        PrintWriter writer = new PrintWriter(dir);
        for (Object o : list) {
            String string = o.toString();
            writer.println(string);
        }
        writer.close();
    }

    /**
     * Same as above but with an array given instead of a list
     *
     * @param dir  where to print
     * @param list what to print
     * @throws Exception
     */
    public static void printFile(File dir, String... list) throws Exception {
        PrintWriter writer = new PrintWriter(dir);
        for (Object o : list) {
            String string = o.toString();
            writer.println(string);
        }
        writer.close();
    }

    /**
     * Checks if a directory exist, if not creates it
     *
     * @param dir to check
     */
    public static void checkDirectory(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
            checkDirectory(dir.getParentFile());
        }
    }

    @SuppressWarnings("resource")
    public static ArrayList<String> getClassNamesFromPackage(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();

        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

        if (packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
//            System.out.println(">" + jarFileName);
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                    entryName = entryName.substring(packageName.length(), entryName.lastIndexOf('.'));
                    names.add(entryName);
                }
            }

        } else {
            URI uri = new URI(packageURL.toString());
            File folder = new File(uri.getPath());
            File[] contenent = folder.listFiles();
            String entryName;
            for (File actual : contenent) {
                entryName = actual.getName();
                entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                names.add(entryName);
            }
        }
        return names;
    }

    public static List<Object> makeList(Iterable<Object> iter) {
        List<Object> list = new ArrayList<Object>();
        for (Object item : iter) {
            list.add(item);
        }
        return list;
    }

    private final static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String calcSHA1(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("sha-1");
        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            return new HexBinaryAdapter().marshal(sha1.digest());
        }
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static String sha1(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            final byte[] buffer = new byte[1024];
            for (int read = 0; (read = is.read(buffer)) != -1; ) {
                messageDigest.update(buffer, 0, read);
            }
        }

        // Convert the byte to hex format
        try (Formatter formatter = new Formatter()) {
            for (final byte b : messageDigest.digest()) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    public static File getWorkingDirectory() {
        String userHome = System.getProperty("user.home", ".");
        File workingDirectory;
        switch (OperatingSystem.getCurrentPlatform()) {
            case LINUX:
                workingDirectory = new File(userHome, ".minecraft/");
                break;
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                String folder = applicationData != null ? applicationData : userHome;

                workingDirectory = new File(folder, ".minecraft/");
                break;
            case OSX:
                workingDirectory = new File(userHome, "Library/Application Support/minecraft");
                break;
            default:
                workingDirectory = new File(userHome, "minecraft/");
        }
        return workingDirectory;
    }

    public enum OperatingSystem {
        LINUX("linux", new String[]{"linux", "unix"}), WINDOWS("windows", new String[]{"win"}), OSX("osx",
                new String[]{"mac"}), UNKNOWN("unknown", new String[0]);

        private final String name;
        private final String[] aliases;

        private OperatingSystem(String name, String... aliases) {
            this.name = name;
            this.aliases = (aliases == null ? new String[0] : aliases);
        }

        public String getName() {
            return this.name;
        }

        public String[] getAliases() {
            return this.aliases;
        }

        public boolean isSupported() {
            return this != UNKNOWN;
        }

        public String getJavaDir() {
            String separator = System.getProperty("file.separator");
            String path = System.getProperty("java.home") + separator + "bin" + separator;
            if ((getCurrentPlatform() == WINDOWS) && (new File(path + "javaw.exe").isFile())) {
                return path + "javaw.exe";
            }
            return path + "java";
        }

        public static OperatingSystem getCurrentPlatform() {
            String osName = System.getProperty("os.name").toLowerCase();
            for (OperatingSystem os : values()) {
                for (String alias : os.getAliases()) {
                    if (osName.contains(alias)) {
                        return os;
                    }
                }
            }
            return UNKNOWN;
        }

        public static void openLink(URI link) {
            try {
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                Object o = desktopClass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
                desktopClass.getMethod("browse", new Class[]{URI.class}).invoke(o, new Object[]{link});
            } catch (Throwable e) {
                if (getCurrentPlatform() == OSX) {
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", link.toString()});
                    } catch (IOException e1) {
//                        System.out.println("Failed to open link " + link.toString());
                    }
                } else {
//                    System.out.println("Failed to open link " + link.toString());
                }
            }
        }

        public static void openFolder(File path) {
            String absolutePath = path.getAbsolutePath();
            OperatingSystem os = getCurrentPlatform();
            if (os == OSX) {
                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", absolutePath});

                    return;
                } catch (IOException e) {
//                    System.out.println("Couldn't open " + path + " through /usr/bin/open");
                }
            } else if (os == WINDOWS) {
                String cmd = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[]{absolutePath});
                try {
                    Runtime.getRuntime().exec(cmd);
                    return;
                } catch (IOException e) {
//                    System.out.println("Couldn't open " + path + " through cmd.exe");
                }
            }
            try {
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                Object desktop = desktopClass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
                desktopClass.getMethod("browse", new Class[]{URI.class}).invoke(desktop,
                        new Object[]{path.toURI()});
            } catch (Throwable e) {
//                System.out.println("Couldn't open " + path + " through Desktop.browse()");
            }
        }
    }
}