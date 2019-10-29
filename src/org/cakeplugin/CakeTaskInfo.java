package org.cakeplugin;

public class CakeTaskInfo {
    private final String command;
    private String name;

    public CakeTaskInfo(String name) {
        this.name = name;
        this.command = String.format("--target=\"%s\"", name);
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return  name;
    }
}
