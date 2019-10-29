package org.cakeplugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class CakeToolWindow extends JBSplitter {
    private static Logger log = Logger.getGlobal();
    private final Project project;
    private final ToolWindow toolWindow;
    private final DefaultMutableTreeNode top;
    private JTree taskTree;
    private final ConsoleView view;

    public CakeToolWindow(@NotNull Project project, ToolWindow toolWindow) {
        super(true, 0.75f);
        this.project = project;
        this.toolWindow = toolWindow;
        setLayout(new BorderLayout());
        top = new DefaultMutableTreeNode("Cake Tasks");
        reloadNodes();
        taskTree = new Tree(top);
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = taskTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = taskTree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1 && e.getClickCount() == 2 && selPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            taskTree.getLastSelectedPathComponent();
                    if (node == null) return;
                    CakeTaskInfo nodeInfo = (CakeTaskInfo) node.getUserObject();
                    try {
                        executeTask(nodeInfo);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        taskTree.addMouseListener(ml);
        JScrollPane treeView = new JBScrollPane(taskTree);
        this.setFirstComponent(treeView);

        TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
        TextConsoleBuilder builder = factory.createBuilder(project);
        view = builder.getConsole();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(view.getComponent(), BorderLayout.CENTER);
        panel.add(new JBLabel("Cake Output"), BorderLayout.NORTH);
        this.setSecondComponent(panel);

    }

    private void executeTask(CakeTaskInfo taskInfo) throws ExecutionException {
        if (taskInfo != null) {
            view.clear();
            GeneralCommandLine commandLine = getCommandLine(taskInfo);
            Process process = commandLine.createProcess();
            ColoredProcessHandler coloredProcessHandler = new KillableColoredProcessHandler(commandLine);
            ProcessTerminatedListener.attach(coloredProcessHandler, project);
            coloredProcessHandler.startNotify();
            view.attachToProcess(coloredProcessHandler);
        }
    }

    @NotNull
    private GeneralCommandLine getCommandLine(CakeTaskInfo taskInfo) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        if (System.getProperty("os.name").startsWith("Windows")) {
            commandLine.setExePath("powershell");
            commandLine.addParameter("./build.ps1");
        } else {
            commandLine.setExePath("./build.sh");
        }
        commandLine.addParameter(taskInfo.getCommand());
        commandLine.setRedirectErrorStream(true);
        commandLine.setWorkDirectory(new File(project.getBasePath()));
        return commandLine;
    }

    public void reloadNodes() {
        top.removeAllChildren();
        List<CakeTaskInfo> cakeTaskInfos = TaskParser.loadTasks(Paths.get(project.getBasePath(), "build.cake"));
        DefaultMutableTreeNode task;
        for (int i = 0; i < cakeTaskInfos.size(); i++) {
            CakeTaskInfo cakeTaskInfo = cakeTaskInfos.get(i);
            task = createNode(cakeTaskInfo);
            top.add(task);
        }
    }

    @NotNull
    private DefaultMutableTreeNode createNode(CakeTaskInfo cakeTaskInfo) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(cakeTaskInfo, false);
        return node;
    }

    public JPanel getContent() {
        return this;
    }
}
