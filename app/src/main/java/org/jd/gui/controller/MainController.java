/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.controller;

import org.apache.commons.io.FilenameUtils;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.gui.api.API;
import org.jd.gui.api.feature.ContentCopyable;
import org.jd.gui.api.feature.ContentIndexable;
import org.jd.gui.api.feature.ContentSavable;
import org.jd.gui.api.feature.ContentSearchable;
import org.jd.gui.api.feature.ContentSelectable;
import org.jd.gui.api.feature.FocusedTypeGettable;
import org.jd.gui.api.feature.IndexesChangeListener;
import org.jd.gui.api.feature.LineNumberNavigable;
import org.jd.gui.api.feature.PreferencesChangeListener;
import org.jd.gui.api.feature.SourcesSavable;
import org.jd.gui.api.feature.UriGettable;
import org.jd.gui.api.model.Container;
import org.jd.gui.api.model.Indexes;
import org.jd.gui.model.configuration.Configuration;
import org.jd.gui.model.history.History;
import org.jd.gui.service.actions.ContextualActionsFactoryService;
import org.jd.gui.service.container.ContainerFactoryService;
import org.jd.gui.service.fileloader.FileLoaderService;
import org.jd.gui.service.indexer.IndexerService;
import org.jd.gui.service.mainpanel.PanelFactoryService;
import org.jd.gui.service.pastehandler.PasteHandlerService;
import org.jd.gui.service.preferencespanel.PreferencesPanelService;
import org.jd.gui.service.sourceloader.Artifact;
import org.jd.gui.service.sourceloader.MavenOrgSourceLoaderProvider;
import org.jd.gui.service.sourceloader.SourceLoaderService;
import org.jd.gui.service.sourcesaver.SourceSaverService;
import org.jd.gui.service.treenode.TreeNodeFactoryService;
import org.jd.gui.service.type.TypeFactoryService;
import org.jd.gui.service.uriloader.UriLoaderService;
import org.jd.gui.spi.ContainerFactory;
import org.jd.gui.spi.FileLoader;
import org.jd.gui.spi.Indexer;
import org.jd.gui.spi.PanelFactory;
import org.jd.gui.spi.PasteHandler;
import org.jd.gui.spi.SourceSaver;
import org.jd.gui.spi.TreeNodeFactory;
import org.jd.gui.spi.TypeFactory;
import org.jd.gui.spi.UriLoader;
import org.jd.gui.util.TempFile;
import org.jd.gui.util.ZOutputStream;
import org.jd.gui.util.container.JarContainerEntryUtil;
import org.jd.gui.util.matcher.ArtifactVersionMatcher;
import org.jd.gui.util.net.UriUtil;
import org.jd.gui.util.swing.AbstractSwingWorker;
import org.jd.gui.util.swing.SwingUtil;
import org.jd.gui.view.MainView;
import org.jd.util.SHA1Util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import tim.jarcomp.CompareWindow;

public class MainController implements API {
    private static final String INDEXES = "indexes";
    private final Configuration configuration;
    @SuppressWarnings("all")
    private MainView mainView;

    private GoToController goToController;
    private OpenTypeController openTypeController;
    private OpenTypeHierarchyController openTypeHierarchyController;
    private PreferencesController preferencesController;
    private SearchInConstantPoolsController searchInConstantPoolsController;
    private SaveAllSourcesController saveAllSourcesController;
    private SelectLocationController selectLocationController;
    private AboutController aboutController;
    private SourceLoaderService sourceLoaderService;

    private final History history = new History();
    private JComponent currentPage;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final List<IndexesChangeListener> containerChangeListeners = new ArrayList<>();

    public MainController(Configuration configuration) {
        this.configuration = configuration;

        SwingUtil.invokeLater(() ->

        // Create main frame
            mainView = new MainView<>(
                configuration, this, history,
                e -> onOpen(),
                e -> onCompare(),
                e -> onClose(),
                e -> onSaveSource(),
                e -> onSaveAllSources(),
                e -> System.exit(0),
                e -> onCopy(),
                e -> onPaste(),
                e -> onSelectAll(),
                e -> onFind(),
                e -> onFindPrevious(),
                e -> onFindNext(),
                e -> onFindCriteriaChanged(),
                this::onFindCriteriaChanged,
                e -> onOpenType(),
                e -> onOpenTypeHierarchy(),
                e -> onGoTo(),
                e -> openURI(history.backward()),
                e -> openURI(history.forward()),
                e -> onSearch(),
                e -> onJdWebSite(),
                e -> onJdGuiIssues(),
                e -> onJdCoreIssues(),
                e -> onPreferences(),
                e -> onAbout(),
                this::panelClosed,
                this::onCurrentPageChanged,
                this::openFile)
        );
    }

    // --- Show GUI --- //
    @SuppressWarnings("unchecked")
    public void show(List<File> files) {
        SwingUtil.invokeLater(() -> {
            // Show main frame
            mainView.show(configuration.getMainWindowLocation(), configuration.getMainWindowSize(), configuration.isMainWindowMaximize());
            if (!files.isEmpty()) {
                openFiles(files);
            }
        });

        // Background initializations
        executor.schedule(() -> {
            // Background service initialization
            UriLoaderService.getInstance();
            FileLoaderService.getInstance();
            ContainerFactoryService.getInstance();
            IndexerService.getInstance();
            TreeNodeFactoryService.getInstance();
            TypeFactoryService.getInstance();

            SwingUtil.invokeLater(() -> {
                // Populate recent files menu
                mainView.updateRecentFilesMenu(configuration.getRecentFiles());
                // Background controller creation
                JFrame mainFrame = mainView.getMainFrame();
                saveAllSourcesController = new SaveAllSourcesController(MainController.this, mainFrame);
                openTypeController = new OpenTypeController(MainController.this, executor, mainFrame);
                containerChangeListeners.add(openTypeController);
                openTypeHierarchyController = new OpenTypeHierarchyController(MainController.this, executor, mainFrame);
                containerChangeListeners.add(openTypeHierarchyController);
                goToController = new GoToController(configuration, mainFrame);
                searchInConstantPoolsController = new SearchInConstantPoolsController(MainController.this, executor, mainFrame);
                containerChangeListeners.add(searchInConstantPoolsController);
                preferencesController = new PreferencesController(configuration, mainFrame, PreferencesPanelService.getInstance().getProviders());
                selectLocationController = new SelectLocationController(MainController.this, mainFrame);
                aboutController = new AboutController(mainFrame);
                sourceLoaderService = new SourceLoaderService();
                // Add listeners
                mainFrame.addComponentListener(new MainFrameListener(configuration));
                // Set drag and drop (DnD) files transfer handler
                mainFrame.setTransferHandler(new FilesTransferHandler());
                // Background class loading
                new JFileChooser().addChoosableFileFilter(new FileNameExtensionFilter("", "dummy"));
                FileSystemView.getFileSystemView().isFileSystemRoot(new File("dummy"));
                @SuppressWarnings({ "rawtypes", "unused" })
                JLayer layer = new JLayer();
            });
        }, 400, TimeUnit.MILLISECONDS);

        PasteHandlerService.getInstance();
        PreferencesPanelService.getInstance();
        ContextualActionsFactoryService.getInstance();
        SourceSaverService.getInstance();
    }

    // --- Actions --- //
    protected void onOpen() {
        Map<String, FileLoader> loaders = FileLoaderService.getInstance().getMapProviders();
        StringBuilder sb = new StringBuilder();
        List<String> extensions = new ArrayList<>(loaders.keySet());

        Collections.sort(extensions);

        for (String extension : extensions) {
            sb.append("*.").append(extension).append(", ");
        }

        sb.setLength(sb.length() - 2);

        String description = sb.toString();
        String[] array = extensions.toArray(new String[0]);
        JFileChooser chooser = new JFileChooser();

        chooser.removeChoosableFileFilter(chooser.getFileFilter());
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("All files (" + description + ")", array));

        for (String extension : extensions) {
            FileLoader loader = loaders.get(extension);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(loader.getDescription(), loader.getExtensions()));
        }

        chooser.setCurrentDirectory(configuration.getRecentLoadDirectory());

        if (chooser.showOpenDialog(mainView.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            configuration.setRecentLoadDirectory(chooser.getCurrentDirectory());
            openFile(chooser.getSelectedFile());
        }
    }

    protected void onCompare() {
        // Construct main window and initialise
        CompareWindow window = new CompareWindow(this);
        // Pass two files to start with, or instruct to prompt
        window.startCompare();
    }

    protected void onClose() {
        mainView.closeCurrentTab();
    }

    protected void onSaveSource() {
        if (currentPage instanceof ContentSavable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentSavable cs = (ContentSavable) currentPage;
            JFileChooser chooser = new JFileChooser();
            JFrame mainFrame = mainView.getMainFrame();

            chooser.setSelectedFile(new File(configuration.getRecentSaveDirectory(), cs.getFileName()));

            if (chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();

                configuration.setRecentSaveDirectory(chooser.getCurrentDirectory());

                if (selectedFile.exists()) {
                    String title = "Are you sure?";
                    String message = "The file '" + selectedFile.getAbsolutePath() + "' already exists.\n Do you want to replace the existing file?";

                    if (JOptionPane.showConfirmDialog(mainFrame, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        save(selectedFile);
                    }
                } else {
                    save(selectedFile);
                }
            }
        }
    }

    protected void save(File selectedFile) {
        try (OutputStream os = new FileOutputStream(selectedFile)) {
            ((ContentSavable) currentPage).save(this, os);
        } catch (FileNotFoundException e) {
            assert ExceptionUtil.printStackTrace(e);
            JOptionPane.showMessageDialog(currentPage, "Not authorized to save to this destination. Please restart as administrator or choose another location.", "Access denied", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            assert ExceptionUtil.printStackTrace(e);
        }
    }

    protected void onSaveAllSources() {
        JComponent currentPanel = mainView.getSelectedMainPanel();
        if (currentPanel instanceof SourcesSavable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            SourcesSavable sourcesSavable = (SourcesSavable) currentPanel;
            JFileChooser chooser = new JFileChooser();
            JFrame mainFrame = mainView.getMainFrame();

            chooser.setSelectedFile(new File(sourcesSavable.getSourceFileName()));

            if (chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();

                configuration.setRecentSaveDirectory(chooser.getCurrentDirectory());

                if (selectedFile.exists()) {
                    String title = "Are you sure?";
                    String message = "The file '" + selectedFile.getAbsolutePath() + "' already exists.\n Do you want to replace the existing file?";

                    if (JOptionPane.showConfirmDialog(mainFrame, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        saveAllSourcesController.show(sourcesSavable, selectedFile);
                    }
                } else {
                    saveAllSourcesController.show(sourcesSavable, selectedFile);
                }
            }
        }
    }

    protected void onCopy() {
        if (currentPage instanceof ContentCopyable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentCopyable cc = (ContentCopyable) currentPage;
            cc.copy();
        }
    }

    protected void onPaste() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object obj = transferable.getTransferData(DataFlavor.stringFlavor);
                PasteHandler pasteHandler = PasteHandlerService.getInstance().get(obj);

                if (pasteHandler != null) {
                    pasteHandler.paste(this, obj);
                }
            }
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
        }
    }

    protected void onSelectAll() {
        if (currentPage instanceof ContentSelectable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentSelectable cs = (ContentSelectable) currentPage;
            cs.selectAll();
        }
    }

    protected void onFind() {
        if (currentPage instanceof ContentSearchable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            mainView.showFindPanel();
        }
    }

    protected void onFindCriteriaChanged() {
        if (currentPage instanceof ContentSearchable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentSearchable cs = (ContentSearchable) currentPage;
            mainView.setFindBackgroundColor(cs.highlightText(mainView.getFindText(), mainView.getFindCaseSensitive()));
        }
    }

    protected void onFindNext() {
        if (currentPage instanceof ContentSearchable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentSearchable cs = (ContentSearchable) currentPage;
            cs.findNext(mainView.getFindText(), mainView.getFindCaseSensitive());
        }
    }

    protected void onOpenType() {
        openTypeController.show(getCollectionOfFutureIndexes(), this::openURI);
    }

    protected void onOpenTypeHierarchy() {
        if (currentPage instanceof FocusedTypeGettable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            FocusedTypeGettable ftg = (FocusedTypeGettable) currentPage;
            openTypeHierarchyController.show(getCollectionOfFutureIndexes(), ftg.getEntry(), ftg.getFocusedTypeName(), this::openURI);
        }
    }

    protected void onGoTo() {
        if (currentPage instanceof LineNumberNavigable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            LineNumberNavigable lnn = (LineNumberNavigable) currentPage;
            goToController.show(lnn, lnn::goToLineNumber);
        }
    }

    protected void onSearch() {
        searchInConstantPoolsController.show(getCollectionOfFutureIndexes(), this::openURI);
    }

    protected void onFindPrevious() {
        if (currentPage instanceof ContentSearchable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentSearchable cs = (ContentSearchable) currentPage;
            cs.findPrevious(mainView.getFindText(), mainView.getFindCaseSensitive());
        }
    }

    protected void onJdWebSite() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("http://java-decompiler.github.io"));
                } catch (IOException e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
        }
    }

    protected void onJdGuiIssues() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("https://github.com/java-decompiler/jd-gui/issues"));
                } catch (IOException e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
        }
    }

    protected void onJdCoreIssues() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create("https://github.com/java-decompiler/jd-core/issues"));
                } catch (IOException e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void onPreferences() {
        preferencesController.show(() -> {
            checkPreferencesChange(currentPage);
            mainView.preferencesChanged(getPreferences());
        });
    }

    protected void onAbout() {
        aboutController.show();
    }

    protected void onCurrentPageChanged(JComponent page) {
        currentPage = page;
        checkPreferencesChange(page);
        checkIndexesChange(page);
    }

    protected void checkPreferencesChange(JComponent page) {
        if (page instanceof PreferencesChangeListener) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            PreferencesChangeListener pcl = (PreferencesChangeListener) page;
            Map<String, String> preferences = configuration.getPreferences();
            Integer currentHashcode = preferences.hashCode();
            Integer lastHashcode = (Integer) page.getClientProperty("preferences-hashCode");
            if (!currentHashcode.equals(lastHashcode)) {
                pcl.preferencesChanged(preferences);
                page.putClientProperty("preferences-hashCode", currentHashcode);
            }
        }
    }

    protected void checkIndexesChange(JComponent page) {
        if (page instanceof IndexesChangeListener) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            IndexesChangeListener icl = (IndexesChangeListener) page;
            Collection<Future<Indexes>> collectionOfFutureIndexes = getCollectionOfFutureIndexes();
            Integer currentHashcode = collectionOfFutureIndexes.hashCode();
            Integer lastHashcode = (Integer) page.getClientProperty("collectionOfFutureIndexes-hashCode");

            if (!currentHashcode.equals(lastHashcode)) {
                icl.indexesChanged(collectionOfFutureIndexes);
                page.putClientProperty("collectionOfFutureIndexes-hashCode", currentHashcode);
            }
        }
    }

    // --- Operations --- //
    public void openFile(File file) {
        openFiles(Collections.singletonList(file));
    }

    public void compareFiles(List<File> files) {
        // Construct main window and initialise
        CompareWindow window = new CompareWindow(this);
        // Pass two files to start with, or instruct to prompt
        window.startCompare(files.get(0), files.get(1));
    }

    public void showGAVs(Set<File> files, Map<File, String> sha1Map, DoubleSupplier getProgressFunction, DoubleConsumer setProgressFunction, BooleanSupplier isCancelledFunction) {
        Set<Artifact> artifacts = new TreeSet<>();
        Set<Artifact> missingArtifacts = new TreeSet<>();
        Set<Artifact> missingArtifactsWithGroup = new TreeSet<>();
        for (File file : files) {
            try {
                String sha1 = sha1Map.computeIfAbsent(file, SHA1Util::computeSHA1);
                Artifact artifact = MavenOrgSourceLoaderProvider.buildArtifactFromURI(file, sha1);
                if (artifact != null && artifact.found()) {
                    artifacts.add(artifact);
                } else {
                    missingArtifacts.add(inferArtifactFromFileName(file));
                    missingArtifactsWithGroup.add(inferArtifactFromPackageAndManifest(file));
                }
                double progress = 100D / files.size();
                double cumulativeProgress = getProgressFunction.getAsDouble() + progress;
                if (cumulativeProgress <= 100) {
                    setProgressFunction.accept(cumulativeProgress);
                }
                if (isCancelledFunction.getAsBoolean()) {
                    return;
                }
            } catch (Exception e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }
        try (TempFile tempFile = new TempFile(".zip")) {
            try (FileOutputStream out = new FileOutputStream(tempFile); ZOutputStream zos = new ZOutputStream(out)) {
                writeGradleBuildEntry(zos, files, artifacts, missingArtifacts);
                writeMavenBuildEntry(zos, artifacts, missingArtifactsWithGroup);
                writeBatchFile(missingArtifactsWithGroup, zos);
            }
            openFiles(Collections.singletonList(tempFile));
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
        }
    }

    private static void writeBatchFile(Set<Artifact> missingArtifactsWithGroup, ZOutputStream zos) throws IOException {
        zos.writeln("@echo off");
        for (Artifact artifact : missingArtifactsWithGroup) {
            if (acceptArtifact(artifact)) {
                zos.write("call mvn deploy:deploy-file -DrepositoryId=%REPO_ID% -Durl=%REPO_URL%");
                zos.write(" -DgroupId=");
                zos.write(artifact.groupId());
                zos.write(" -Dfile=");
                zos.write(artifact.fileName());
                zos.write(" -DartifactId=");
                zos.write(artifact.artifactId());
                zos.write(" -Dversion=");
                zos.writeln(artifact.version());
            }
        }
        zos.closeEntry();
    }

    private static boolean acceptArtifact(Artifact a) {
        return !a.fileName().contains("sources") && !a.fileName().contains("SNAPSHOT");
    }

    private static void writeMavenBuildEntry(ZOutputStream zos, Set<Artifact> artifacts, Set<Artifact> missingArtifactsWithGroup) throws IOException {
        zos.putNextEntry(new ZipEntry("pom.xml"));
        zos.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <dependencies>
                                """);
        zos.writeln("    <!-- Thirdparty libraries available on maven central -->");
        for (Artifact artifact : artifacts) {
            writeMavenDependency(zos, artifact);
        }
        zos.writeln("    <!-- Vendor libraries and other libraries without sources -->");
        for (Artifact artifact : missingArtifactsWithGroup) {
            if (acceptArtifact(artifact)) {
                writeMavenDependency(zos, artifact);
            }
        }
        zos.write("""
                </dependencies>
                </project>
                                """);
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("mvn_deploy.bat"));
    }

    private static void writeGradleBuildEntry(ZOutputStream zos, Set<File> files, Set<Artifact> artifacts, Set<Artifact> missingArtifacts) throws IOException {
        zos.putNextEntry(new ZipEntry("build.gradle"));
        zos.writeln("plugins {");
        zos.writeln("    id 'java'");
        zos.writeln("}");
        zos.writeln("");
        zos.writeln("repositories {");
        zos.writeln("    mavenCentral()");
        zos.writeln("    flatDir {");
        zos.write("        dirs ");
        Set<String> dirs = files.stream().map(File::getParent).collect(Collectors.toSet());
        for (Iterator<String> it = dirs.iterator(); it.hasNext();) {
            String dir = it.next();
            zos.write("\"");
            if (dir == null) {
                zos.write("lib");
            } else {
                zos.write(dir);
            }
            zos.write("\"");
            if (it.hasNext()) {
                zos.write(", ");
            }
        }
        zos.writeln("");
        zos.writeln("    }");
        zos.writeln("}");
        zos.writeln("");
        zos.writeln("dependencies {");
        if (!artifacts.isEmpty()) {
            zos.writeln("    // Thirdparty libraries available on maven central");
        }
        for (Artifact artifact : artifacts) {
            zos.write("    compile('");
            zos.write(artifact.groupId());
            zos.write(":");
            zos.write(artifact.artifactId());
            zos.write(":");
            zos.write(artifact.version());
            zos.writeln("') { transitive = false }");
        }
        if (!missingArtifacts.isEmpty()) {
            zos.writeln("    // Vendor libraries and other libraries without sources");
        }
        for (Artifact artifact : missingArtifacts) {
            if (acceptArtifact(artifact)) {
                zos.write("    compile ':");
                zos.write(artifact.artifactId());
                if (!artifact.version().isEmpty()) {
                    zos.write(":");
                    zos.write(artifact.version());
                }
                zos.writeln("'");
            }
        }
        zos.write("}");
        zos.closeEntry();
    }

    private static void writeMavenDependency(ZOutputStream zos, Artifact artifact) throws IOException {
        zos.writeln("    <dependency>");
        zos.write("      <groupId>");
        zos.write(artifact.groupId());
        zos.writeln("</groupId>");
        zos.write("      <artifactId>");
        zos.write(artifact.artifactId());
        zos.writeln("</artifactId>");
        zos.write("      <version>");
        zos.write(artifact.version());
        zos.writeln("</version>");
        zos.writeln("    </dependency>");
    }

    private static Artifact inferArtifactFromPackageAndManifest(File file) {
        String baseFileName = FilenameUtils.getBaseName(file.getName());
        ArtifactVersionMatcher artifactVersionMatcher = new ArtifactVersionMatcher();
        artifactVersionMatcher.parse(baseFileName);
        if (file.exists()) {
            try (JarFile jarFile = new JarFile(file)) {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    String artifactId = artifactVersionMatcher.getArtifactId();
                    String version = artifactVersionMatcher.getVersion();
                    return new Artifact(artifactId, artifactId, version, file.getName(), false, false);
                }
                Attributes mainAttributes = manifest.getMainAttributes();
                String groupId = mainAttributes.getValue("Implementation-Vendor-Id");
                if (groupId == null) {
                    groupId = mainAttributes.getValue("Bundle-SymbolicName");
                }
                if (groupId == null) {
                    groupId = JarContainerEntryUtil.inferGroupFromFile(jarFile);
                }
                String version = mainAttributes.getValue("Implementation-Version");
                if (version == null) {
                    version = mainAttributes.getValue("Bundle-Version");
                }
                if (version == null) {
                    version = artifactVersionMatcher.getVersion();
                }
                String artifactId = artifactVersionMatcher.getArtifactId();
                return new Artifact(groupId, artifactId, version, file.getName(), false, false);
            } catch (IOException e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        } else {
            String artifactId = artifactVersionMatcher.getArtifactId();
            String version = artifactVersionMatcher.getVersion();
            return new Artifact("com.mycompany", artifactId, version, file.getName(), false, false);
        }
        return null;
    }

    private static Artifact inferArtifactFromFileName(File file) {
        String baseFileName = FilenameUtils.getBaseName(file.getName());
        ArtifactVersionMatcher artifactVersionMatcher = new ArtifactVersionMatcher();
        artifactVersionMatcher.parse(baseFileName);
        String artifactId = artifactVersionMatcher.getArtifactId();
        String version = artifactVersionMatcher.getVersion();
        return new Artifact(artifactId, artifactId, version, file.getName(), false, false);
    }

    @SuppressWarnings("unchecked")
    public void openFiles(List<File> files) {
        List<String> errors = new ArrayList<>();

        for (File file : files) {
            // Check input file
            if (file.exists()) {
                FileLoader loader = getFileLoader(file);
                if (loader != null && !loader.accept(this, file)) {
                    errors.add("Invalid input fileloader: '" + file.getAbsolutePath() + "'");
                }
            } else {
                errors.add("File not found: '" + file.getAbsolutePath() + "'");
            }
        }

        if (errors.isEmpty()) {
            for (File file : files) {
                if (openURI(file.toURI())) {
                    configuration.addRecentFile(file);
                    mainView.updateRecentFilesMenu(configuration.getRecentFiles());
                }
            }
        } else {
            StringBuilder messages = new StringBuilder();
            int index = 0;

            for (String error : errors) {
                if (index > 0) {
                    messages.append('\n');
                }
                if (index >= 20) {
                    messages.append("...");
                    break;
                }
                messages.append(error);
                index++;
            }

            JOptionPane.showMessageDialog(mainView.getMainFrame(), messages.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private final class IndexerWorker extends AbstractSwingWorker<Indexes, Void> {
        private final ContentIndexable ci;

        private IndexerWorker(Component component, ContentIndexable ci) {
            super(component, "Indexing ...");
            this.ci = ci;
        }

        @Override
        protected Indexes doInBackground() throws Exception {
            return ci.index(MainController.this, this::getProgressPercentage, this::setProgressPercentage, this::isCancelled);
        }

        @Override
        protected void done() {
            super.done();

            // Fire 'indexesChanged' event
            Collection<Future<Indexes>> collectionOfFutureIndexes = getCollectionOfFutureIndexes();
            for (IndexesChangeListener listener : containerChangeListeners) {
                listener.indexesChanged(collectionOfFutureIndexes);
            }
            if (currentPage instanceof IndexesChangeListener) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                IndexesChangeListener icl = (IndexesChangeListener) currentPage;
                icl.indexesChanged(collectionOfFutureIndexes);
            }
        }
    }

    private final class GAVWorker extends AbstractSwingWorker<Void, Void> {
        private final Set<File> files;
        private final Map<File, String> sha1Map;

        private GAVWorker(Component component, Set<File> files, Map<File, String> sha1Map) {
            super(component, "Generating POM ...");
            this.files = files;
            this.sha1Map = sha1Map;
        }

        @Override
        protected Void doInBackground() throws Exception {
            showGAVs(files, sha1Map, this::getProgressPercentage, this::setProgressPercentage, this::isCancelled);
            return null;
        }
    }

    // --- Drag and Drop (DnD) files transfer handler --- //
    protected class FilesTransferHandler extends TransferHandler {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferHandler.TransferSupport info) {
            if (info.isDrop() && info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                try {
                    List<File> files = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    switch (files.size()) {
                        case 1:
                            File file = files.get(0);
                            if ("sha1.txt".equals(file.getName())) {
                                Map<File, String> sha1Map = SHA1Util.readSHA1File(file);
                                launchGAVWorker(info, sha1Map.keySet(), sha1Map);
                            } else {
                                openFiles(files);
                            }
                            break;
                        case 2:
                            compareFiles(files);
                            break;
                        default:
                            launchGAVWorker(info, new HashSet<>(files), new HashMap<>());
                            break;
                    }
                    return true;
                } catch (Exception e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
            return false;
        }

        private void launchGAVWorker(TransferHandler.TransferSupport info, Set<File> files, Map<File, String> sha1Map) {
            SwingWorker<Void, Void> worker = new GAVWorker(info.getComponent(), files, sha1Map);
            worker.execute();
        }
    }

    // --- ComponentListener --- //
    protected class MainFrameListener extends ComponentAdapter {
        private final Configuration configuration;

        public MainFrameListener(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            JFrame mainFrame = mainView.getMainFrame();

            if ((mainFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                configuration.setMainWindowMaximize(true);
            } else {
                configuration.setMainWindowLocation(mainFrame.getLocation());
                configuration.setMainWindowMaximize(false);
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            JFrame mainFrame = mainView.getMainFrame();

            if ((mainFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                configuration.setMainWindowMaximize(true);
            } else {
                configuration.setMainWindowSize(mainFrame.getSize());
                configuration.setMainWindowMaximize(false);
            }
        }
    }

    protected void panelClosed() {
        SwingUtil.invokeLater(() -> {
            // Fire 'indexesChanged' event
            Collection<Future<Indexes>> collectionOfFutureIndexes = getCollectionOfFutureIndexes();
            for (IndexesChangeListener listener : containerChangeListeners) {
                listener.indexesChanged(collectionOfFutureIndexes);
            }
            if (currentPage instanceof IndexesChangeListener) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                IndexesChangeListener icl = (IndexesChangeListener) currentPage;
                icl.indexesChanged(collectionOfFutureIndexes);
            }
        });
    }

    // --- API --- //
    @Override
    public boolean openURI(URI uri) {
        if (uri != null) {
            boolean success = mainView.openUri(uri);

            if (!success) {
                UriLoader uriLoader = getUriLoader(uri);
                if (uriLoader != null) {
                    success = uriLoader.load(this, uri);
                }
            }

            if (success) {
                addURI(uri);
            }

            return success;
        }

        return false;
    }

    @Override
    public boolean openURI(int x, int y, Collection<Container.Entry> entries, String query, String fragment) {
        if (entries != null) {
            if (entries.size() == 1) {
                // Open the single entry uri
                Container.Entry entry = entries.iterator().next();
                return openURI(UriUtil.createURI(this, getCollectionOfFutureIndexes(), entry, query, fragment));
            }
            // Multiple entries -> Open a "Select location" popup
            Collection<Future<Indexes>> collectionOfFutureIndexes = getCollectionOfFutureIndexes();
            selectLocationController.show(
                new Point(x+16+2, y+2),
                entries,
                entry -> openURI(UriUtil.createURI(this, collectionOfFutureIndexes, entry, query, fragment)), // entry selected closure
                () -> {});                                                                                    // popup close closure
            return true;
        }

        return false;
    }

    @Override
    public void addURI(URI uri) {
        history.add(uri);
        SwingUtil.invokeLater(() -> mainView.updateHistoryActions());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends JComponent & UriGettable> void addPanel(File file, String title, Icon icon, String tip, T component) {
        mainView.addMainPanel(title, icon, tip, component);

        if (component instanceof ContentIndexable && file != null) {
            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ContentIndexable ci = (ContentIndexable) component;
            UIManager.put("ProgressMonitor.progressText", title);
            SwingWorker<Indexes, Void> worker = new IndexerWorker(component, ci);
            worker.execute();

            component.putClientProperty(INDEXES, worker);
        }
    }

    @Override
    public Collection<Action> getContextualActions(Container.Entry entry, String fragment) {
        return ContextualActionsFactoryService.getInstance().get(this, entry, fragment);
    }

    @Override
    public FileLoader getFileLoader(File file) {
        return FileLoaderService.getInstance().get(file);
    }

    @Override
    public UriLoader getUriLoader(URI uri) {
        return UriLoaderService.getInstance().get(this, uri);
    }

    @Override
    public PanelFactory getMainPanelFactory(Container container) {
        return PanelFactoryService.getInstance().get(container);
    }

    @Override
    public ContainerFactory getContainerFactory(Path rootPath) {
        return ContainerFactoryService.getInstance().get(this, rootPath);
    }

    @Override
    public TreeNodeFactory getTreeNodeFactory(Container.Entry entry) {
        return TreeNodeFactoryService.getInstance().get(entry);
    }

    @Override
    public TypeFactory getTypeFactory(Container.Entry entry) {
        return TypeFactoryService.getInstance().get(entry);
    }

    @Override
    public Indexer getIndexer(Container.Entry entry) {
        return IndexerService.getInstance().get(entry);
    }

    @Override
    public SourceSaver getSourceSaver(Container.Entry entry) {
        return SourceSaverService.getInstance().get(entry);
    }

    @Override
    public Map<String, String> getPreferences() {
        return configuration.getPreferences();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Future<Indexes>> getCollectionOfFutureIndexes() {
        List<JComponent> mainPanels = mainView.getMainPanels();
        List<Future<Indexes>> list = new ArrayList<>(mainPanels.size()) {

            private static final long serialVersionUID = 1L;

            @Override
            public int hashCode() {
                int hashCode = 1;
                try {
                    for (Future<Indexes> futureIndexes : this) {
                        hashCode *= 31;
                        if (futureIndexes.isDone()) {
                            hashCode += futureIndexes.get().hashCode();
                        }
                    }
                } catch (InterruptedException e) {
                    assert ExceptionUtil.printStackTrace(e);
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
                return hashCode;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || o.getClass() != this.getClass()) {
                    return false;
                }
                return super.equals(o);
            }
        };
        for (JComponent panel : mainPanels) {
            Future<Indexes> futureIndexes = (Future<Indexes>) panel.getClientProperty(INDEXES);
            if (futureIndexes != null) {
                list.add(futureIndexes);
            }
        }
        return list;
    }

    @Override
    public Collection<Indexes> getCollectionOfIndexes() {
        @SuppressWarnings("unchecked")
        List<JComponent> mainPanels = mainView.getMainPanels();
        List<Indexes> list = new ArrayList<>(mainPanels.size());
        for (JComponent panel : mainPanels) {
            @SuppressWarnings("unchecked")
            Future<Indexes> indexes = (Future<Indexes>) panel.getClientProperty(INDEXES);
            if (indexes != null) {
                try {
                    list.add(indexes.get());
                } catch (InterruptedException e) {
                    assert ExceptionUtil.printStackTrace(e);
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
        }
        return list;
    }

    @Override
    public String getSource(Container.Entry entry) {
        return sourceLoaderService.getSource(this, entry);
    }

    @Override
    public void loadSource(Container.Entry entry, LoadSourceListener listener) {
        executor.execute(() -> {
            String source = sourceLoaderService.loadSource(this, entry);

            if (source != null && !source.isEmpty()) {
                listener.sourceLoaded(source);
            }
        });
    }

    @Override
    public File loadSourceFile(Container.Entry entry) {
        return sourceLoaderService.getSourceFile(this, entry);
    }

    @Override
    public void repaint() {
        mainView.repaint();
    }
}
