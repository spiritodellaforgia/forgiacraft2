package me.nether.forgiagent;

import com.google.gson.*;
import me.nether.forgiagent.utils.FileUtils;
import me.nether.forgiagent.utils.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ForgiaAgent {

    private static final boolean DEV_ENABLED = true;

    /**
     * Used to log into the swing gui, maybe could have done it in a better way: see log(Object... obj) method for reference
     */
    public static final List<String> console = new ArrayList<>();

    /**
     * Raw manifest content with all mods/configs etc
     */
    private static final String manifest_url = "https://raw.githubusercontent.com/spiritodellaforgia/forgiacraft2/master/manifest.json";

    /**
     * Used to setup the java arguments other than the javaagent: part
     */
    private static final String argsFormat = "-Xmx%sM -Xmn128M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -javaagent:%s";

    /**
     * Arrays used to store and copy the old files to a temporary directory
     */
    private static List<File> mods = new ArrayList<>(), toMove = new ArrayList<>();

    private static File forgeFile = null;

    /**
     * Main method, launched when the Installer is started manually:
     * the gui shows the parameters that do setup the installation.
     * Console is also included
     * See me.nether.forgiagent.Installer.loadWindow(boolean showInstaller) method for reference
     *
     * @param args none required
     */
    public static void main(String args[]) {
        (Installer.instance = new Installer(true)).loadWindow();

        //TODO: Save autohide status on file before implementing
        Installer.instance.autoHide.setState(false);
        Installer.instance.autoHide.setVisible(false);

        //TODO: gamedir fix
        String gameDir = FileUtils.getWorkingDirectory().getAbsolutePath();

        Rectangle oldBounds = Installer.instance.frame.getBounds();
        Installer.instance.frame.setBounds(oldBounds.x, oldBounds.y, oldBounds.width, oldBounds.height + 50);

        download(new File(gameDir), manifest_url, true, "forge");

        Installer.instance.frame.setBounds(oldBounds);
        Installer.instance.downloadStatus.setVisible(false);

        log("Download completed");
    }

    /**
     * Installer method
     *
     * Reads up launcher_profiles from MC launcher to modify and add the java arguments required to be launched
     * with the usual MC start up
     *
     * The file copies himself into the MC directory since it does contain everything to start before the launcher
     * directly within itself, without the need to download another file
     *
     * @param directory MC game directory location
     * @return success value
     * @throws Exception
     */
    public static boolean install(File directory) throws Exception {
        File profiles_json = new File(directory, "launcher_profiles.json");
        File agentFile = new File(directory, "forgiagent.jar");

        if (!Installer.instance.console.getText().contains("Download completed")) {
            JOptionPane.showMessageDialog(Installer.instance.frame, "Please wait until forge is downloaded, please check console!");
            return false;
        }

        try {
            Process peepee = Runtime.getRuntime().exec("java -jar " + forgeFile.getAbsolutePath());
            log("Launching forge installer...");

            /* Logging forge installer process */
            String readLine;
            BufferedReader br = new BufferedReader(new InputStreamReader(peepee.getInputStream()));
            while (((readLine = br.readLine()) != null)) {
                System.out.println(readLine);
            }

            peepee.waitFor();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(Installer.instance.frame, Installer.instance.errors_type.isSelected() ?
                    "Forge installer is somehow bugged, retry" :
                    "Non ho trovato in buono stato l'installer di forge, riprova.");

            ex.printStackTrace();
            return false;
        }

        Installer.instance.profiles = Installer.instance.getProfiles(directory);

        ForgiaAgent.log("Logging profiles...");
        Installer.instance.profiles.forEach((s, s2) -> System.out.println(s + ": " + s2));

        if (!Installer.instance.profiles.containsValue("forge")) {
            log("No forge profile found! Something must have gone wrong with the forge installer. Please retry");

            JOptionPane.showMessageDialog(Installer.instance.frame, Installer.instance.errors_type.isSelected() ?
                    "No forge profile found! Something must have gone wrong with the forge installer. Please retry" :
                    "Zio hai fatto cagate con l'installer di forge, riprova.");

            return false;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser p = new JsonParser();
        JsonObject main = p.parse(new FileReader(profiles_json)).getAsJsonObject();
        JsonObject profiles = main.get("profiles").getAsJsonObject();
        JsonObject current = profiles.getAsJsonObject("forge");

        log("Replacing javaArgs value: adding the whole string...");

        current.addProperty("javaArgs", String.format(argsFormat, Installer.instance.dedicatedRam.getText(), agentFile.getPath()));

        current.remove("gameDir");
        current.addProperty("gameDir", Installer.instance.modpack_directory.getText());

        /** resetting lastUsed to have it first on a new launch */
        current.remove("lastUsed");
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd#HH:mm:ss.420_");
        current.addProperty("lastUsed", dateFormat.format(new Date()).replaceAll("#", "T").replaceAll("_", "Z"));

        current.remove("name");
        current.addProperty("name", "ForgiaModPack 2.0");

        try {
            FileUtils.copyFile(FileUtils.getCurrentJarPath(), agentFile);
        } catch (Exception ex) {
            log("Couldn't copy the agent to his correct folder.");
            ex.printStackTrace();
        }

        FileUtils.printFile(profiles_json, gson.toJson(main));

        return true;
    }

    /**
     * Method used when launched as a Java Agent, so after the setup is done and the MC launcher is loading the game
     *
     * Launcher download method that checks the current online manifest.json with the one stored locally
     * to see if somethings needs to be updated or downloaded fresh new
     *
     * then launches the hideMods method which stores unused/old mods into a temporary directory
     * the user will then decide to delete them or do something else with them
     *
     * @param agentArgs usually none
     * @param inst      same as above
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        log("Loaded as agent!");

        (Installer.instance = new Installer(false)).loadWindow();

        Installer.instance.autoHide.setState(false);
        Installer.instance.autoHide.setVisible(false);

        Installer.instance.downloadStatus.setVisible(true);
        Installer.instance.progressBar.setVisible(true);

        String args = System.getProperty("sun.java.command");
        String split[] = args.split("--gameDir ");


        String gameDir = split[1].split("--")[0].trim();
        log(String.format("Found gameDir: %s . Working on it to setup all the mods correctly...", gameDir));
        File modsFolder = new File(gameDir, "mods");

        FileUtils.checkDirectory(modsFolder);

        download(new File(gameDir), manifest_url, false);

        Installer.instance.frame.setSize(Installer.instance.frame.getWidth(), Installer.instance.frame.getHeight() - 69);

        if (Installer.instance.autoHide.getState()) {
            Installer.instance.frame.setVisible(false);
        }

        log("Finished setup.");
    }

    /**
     * Downloads everything given the current manifest
     *
     * Stores information about the download in a new manifest file on disk
     * It is updated with any file downloaded, so it only stores the files that are
     * effectively present on disk.
     *
     * @param gamedir       Current Minecraft directory for the profile
     * @param manifest_url  manifest.json url from the repository
     * @param onlyForced    Used by installer to get the forge jar, forces download of forceChecks folders (if true the normal files are skipped)
     * @param forceChecks   Array of folders to forceupdate
     */
    private static void download(File gamedir, String manifest_url, boolean onlyForced, String... forceChecks) {
        try {
            /* old manifest file, doesn't need to be effectively present, checked later */
            File old_manifest_file = new File(gamedir, "manifest.json");

            /* old (on disk), current (on github), new (will replace old on disk) */
            JsonObject old_manifest = old_manifest_file.exists() ? new JsonParser().parse(new FileReader(old_manifest_file)).getAsJsonObject() : null;
            JsonObject current_manifest = getJsonObject(manifest_url);
            JsonObject new_manifest = new JsonObject();

            /* hashes & files arrays from the online json */
            JsonArray current_hashes = current_manifest.get("hashes").getAsJsonArray();
            JsonArray files = new JsonArray();

            /* Collects the folders that have something wrong and need update, speeds up time (not looking for every file every time) */
            List<String> toUpdate = getFoldersToUpdate(current_manifest, old_manifest, current_hashes, gamedir);

            /* Starts to build the new manifest */
            new_manifest.add("hashes", current_hashes);

            log();

            /* Looping the files to meet something that needs update */
            current_manifest.get("files").getAsJsonArray().forEach(jsonElement -> {
                JsonObject obj = jsonElement.getAsJsonObject();

                /* Removing a folder that is not needed (and maybe harmful for the download/load process) */
                String path = obj.get("path").getAsString().replaceAll("forgiacraft2/", "");

                /* Getting only the first folder that corresponds to "mods", "config" or "forge" */
                String folder = path.split("/")[0];

                if ((onlyForced && Arrays.asList(forceChecks).contains(folder)) || (!onlyForced && toUpdate.contains(folder))) {
                    long size = obj.get("size").getAsLong();
                    String hash = obj.get("hash").getAsString();

                    File file = new File(gamedir, path);

                    if (obj.get("path").getAsString().contains("/forge/")) {
                        forgeFile = file;
                    }

                    if (needsUpdate(file, old_manifest_file, old_manifest, obj, size, hash)) {
                        performDownload(path, obj.get("download_url").getAsString(), file);
                    }

                    try {
                        files.add(obj);

                        if (new_manifest.get("files") != null) {
                            new_manifest.remove("files");
                        }

                        new_manifest.add("files", files);

                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        FileUtils.printFile(old_manifest_file, gson.toJson(new_manifest));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            });
        } catch (Exception ex) {
            log(gamedir.getAbsolutePath());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log(sw.toString());
            ex.printStackTrace();
        }
    }

    /**
     * Downloads a file
     *
     * @param path          path from the manifest
     * @param download_url  github raw download
     * @param file          where it will be downloaded
     */
    private static void performDownload(String path, String download_url, File file) {
        try {
            log("Downloading", path, "...");

            FileUtils.checkDirectory(file.getParentFile());

            Thread t = new Thread(() -> {
                try {
                    NetworkUtils.downloadUsingStream(download_url, file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            t.start();
            t.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static List<String> getFoldersToUpdate(JsonObject manifest, JsonObject old_manifest, JsonArray hashes, File gamedir) {
        List<String> toUpdate = new ArrayList<>();

        try {
            JsonArray old_hashes = old_manifest.get("hashes").getAsJsonArray();

            for (JsonElement h : hashes) {
                JsonObject hObj = h.getAsJsonObject();
                String name, hash;
                log("\n\t", (name = hObj.get("name").getAsString()), (hash = hObj.get("hash").getAsString()));

                for (JsonElement h1 : old_hashes) {
                    JsonObject hObj1 = h1.getAsJsonObject();
                    String name1 = hObj1.get("name").getAsString();
                    String hash1 = hObj.get("hash").getAsString();

                    if (name.equals(name1)) {
                        if (!hash.equals(hash1)) {
                            toUpdate.add(name);

                            log(name + " needs to be updated (hash check)");
                        } else {
                            long folder_size = 0;
                            int count = 0;

                            for (JsonElement jsonElement : manifest.get("files").getAsJsonArray()) {

                                JsonObject obj = jsonElement.getAsJsonObject();
                                String path = obj.get("path").getAsString().replaceAll("forgiacraft2/", "");

                                String folder = path.split("/")[0];

                                if (folder.equals(name)) {
                                    folder_size += Integer.parseInt(obj.get("size").getAsString());
                                    count++;
                                }
                            }

                            File dir = new File(gamedir, name);

                            log("Directory should contain", count, "files.", "Currently has: ", FileUtils.listFiles(dir).size(), "file(s)");

                            long actual_size = FileUtils.getDirSize(dir);

                            if (folder_size != actual_size) {
                                toUpdate.add(name);

                                log(String.format(name + " needs to be updated (size check: %s != %s)", folder_size, actual_size));
                            } else
                                log(name + " is up to date");
                        }

                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

            log(gamedir);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log(sw.toString());

            log("No manifest.json found... Doing a full update");

            toUpdate.add("forge");
            toUpdate.add("mods");
            toUpdate.add("config");
        }

        return toUpdate;
    }

    /**
     * Checks if a file needs update
     * Used checks: file is present, size is correct, hash is present, hash is correct
     * @param file
     * @param oldFile
     * @param old_manifest
     * @param obj
     * @param size
     * @param hash
     * @return
     */
    private static boolean needsUpdate(File file, File oldFile, JsonObject old_manifest, JsonObject obj, long size, String hash) {
        try {
            boolean check1 = !oldFile.exists();
            if (check1) return true;
            boolean check2 = !file.exists();
            if (check2) return true;
            boolean check3 = file.length() != size;
            if (check3) return true;
            boolean check4 = old_manifest == null;
            if (check4) return true;
            boolean check5 = old_manifest.get("files") == null;
            if (check5) return true;
            boolean check6 = getHashFromPath(old_manifest.get("files").getAsJsonArray(), obj.get("path").getAsString()) == null;
            if (check6) return true;
            boolean check7 = !getHashFromPath(old_manifest.get("files").getAsJsonArray(), obj.get("path").getAsString()).equals(hash);
            if (check7) return true;

            return false;
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log(sw.toString());
            return true;
        }
    }

    /**
     * Gets the hash of a File given the JsonArray containing its data
     * Used for commodity
     *
     * @param array    JsonArray with the data
     * @param filePath Name of the file
     * @return file hashing from github modified sha1
     */
    private static String getHashFromPath(JsonArray array, String filePath) {
        try {
            if (array == null) return null;

            for (JsonElement jsonElement : array) {
                JsonObject file_data = jsonElement.getAsJsonObject();

                String path = file_data.get("path").getAsString();
                String hash = file_data.get("hash").getAsString();

                if (filePath.equals(path)) {
                    return hash;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Gets a json object from an url that leads to a json file
     *
     * @param request_url
     * @return
     */
    private static JsonObject getJsonObject(String request_url) {
        JsonObject jsonResponse = new JsonParser().parse(NetworkUtils.getResponseBody(request_url)).getAsJsonObject();
        return jsonResponse;
    }

    /**
     * Used for logging purposes, copies content to the Swing GUI as well
     *
     * @param obj an array of object to be logged after a .toString() is applied
     */
    public static void log(Object... obj) {
        StringBuilder sb = new StringBuilder();

        Arrays.asList(obj).forEach(o -> sb.append(o.toString() + " "));

        String toLog = sb.toString();

        System.out.println("[ForgiaAgent]: " + toLog.toString());
        console.add(toLog);
        try {
            StringBuilder builder = new StringBuilder();
            ForgiaAgent.console.forEach(s -> builder.append(s + "\n"));
            Installer.instance.console.setText(builder.toString());

            Installer.instance.console.selectAll();
            int x = Installer.instance.console.getSelectionEnd();
            Installer.instance.console.select(x, x);

        } catch (Exception ex) {
        }
    }

    /**
     * Hide unused/old mods in a temporary folder
     *
     * @param modsFolder effective mods folder
     * @param where      temporary folder which remains unused
     */
    @Deprecated
    private static void hideMods(File modsFolder, File where) {
        FileUtils.checkDirectory(where);
        toMove.clear();

        List<File> allMods = FileUtils.getAllFilesInFolder(modsFolder, false);

        //moves in another folder all the old/unused mods.
        allMods.stream().filter(file -> !mods.contains(file)).forEach(file -> {
            try {
                log("Moving " + file.getName() + " to a temporary location...");
                FileUtils.copyFile(file, new File(where, file.getName()));
                file.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
