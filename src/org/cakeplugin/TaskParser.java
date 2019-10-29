package org.cakeplugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TaskParser {
    private static String loadPattern = "#load \"";

    public static List<CakeTaskInfo> loadTasks(Path configPath) {
        List<CakeTaskInfo> list = new LinkedList<>();
        try {
            if (configPath != null) {
                File file = configPath.toFile();
                if (file.exists() && file.isFile()) {
                    //Parse the Cake script with all its loads
                    ScriptContent script = new ScriptContent(configPath);
                    script.parse(loadPattern, s -> s.replace("#load", "")
                            .trim()
                            .replaceAll("^\"|\"$", "")
                            .replaceAll(";", ""));
                    String document = script.toString();
                    //Extract task names
                    Pattern r = Pattern.compile("Task\\s*\\(\\s*\\\"(.+)\\b\\\"\\s*\\)");
                    Matcher matches = r.matcher(document);
                    List<String> taskNames = new LinkedList<>();
                    while (matches.find()) {
                        String task = matches.group();
                        String taskName = task.replaceAll("Task\\s*\\(\\s*\\\"", "")
                                .replaceAll("\\\"\\s*\\)", "");
                        taskNames.add(taskName);
                    }
                    Collections.sort(taskNames);
                    taskNames.forEach(name ->
                            list.add(new CakeTaskInfo(name)));
                }
            }
        } catch (Exception ex) {
            Logger.getGlobal().severe(ex.toString());
        }
        return list;
    }
}
