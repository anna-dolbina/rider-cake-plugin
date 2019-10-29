package org.cakeplugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ScriptContent {
    private ArrayList<String> content;
    private Map<Integer, ScriptContent> loads = new HashMap<>();
    private Path filePath;
    ScriptContent(Path filePath) throws IOException {
        this.filePath = filePath;
        content = new ArrayList<>(Files.lines(this.filePath).collect(Collectors.toList()));
    }

    void parse(String pattern, Function<String, String> stripFunc) {
        content.stream().filter(l -> l.startsWith(pattern))
                .forEach(line -> {
                    Path parent = filePath.getParent();
                    if (parent == null) {
                        //parent = project.getBaseDir().getPath();
                    }
                    String dirPath = parent.toAbsolutePath().toString();
                    Path path = Paths.get(dirPath, stripFunc.apply(line));
                    try {
                        loads.put(content.indexOf(line), new ScriptContent(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        loads.forEach((i, load) -> {
            load.parse(pattern, stripFunc);
        });
    }

    public String toString() {
        loads.forEach((i, load) -> {
            content.add(i, load.toString());
        });
        //Remove empty lines
        return content.stream()
                .map(c -> String.join(System.lineSeparator(), c.split(System.lineSeparator())))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
