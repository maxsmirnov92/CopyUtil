package net.maxsmr.copyutil;

import net.maxsmr.copyutil.utils.ArgsParser;
import net.maxsmr.copyutil.utils.CompareUtils;
import net.maxsmr.copyutil.utils.FileHelper;
import net.maxsmr.copyutil.utils.Pair;
import net.maxsmr.copyutil.utils.Predicate;
import net.maxsmr.copyutil.utils.StreamUtils;
import net.maxsmr.copyutil.utils.TextUtils;
import net.maxsmr.copyutil.utils.logger.BaseLogger;
import net.maxsmr.copyutil.utils.logger.SimpleSystemLogger;
import net.maxsmr.copyutil.utils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.copyutil.utils.Units.timeToString;

public class CopyUtil {

    // new File("") == jar working dir

    private static final BaseLogger logger;

    private static final String lineSeparator = "line.separator";

    private static final String[] argsNames =
            {"-pathsListFile", "-sourcePath", "-destinationPath", "-renameFiles", "-deleteEmptyDirs", "-deleteCopiedFiles", "-ignoreExcludedPaths", "-excludeSourcePathsFile", "-forceOverwrite", "-disableRecursion"};

    private static final String[] excludedPaths =
            {"Boot", "Documents and Settings", "ProgramData", "Program Files", "Program Files (x86)", "Recovery", "System Volume Information", "Windows", "Users"};


    static {
        System.setErr(System.out);
        BaseLoggerHolder.initInstance(() -> new BaseLoggerHolder(false) {
            @Override
            protected BaseLogger createLogger(Class<?> clazz) {
                if (clazz != FileHelper.class) {
                    return new SimpleSystemLogger();
                } else {
                    return new BaseLogger.Stub();
                }
            }
        });
        logger = BaseLoggerHolder.getInstance().getLogger(CopyUtil.class);
    }

    private static ArgsParser argsParser;

    private static File pathsToHandleListFile;
    private static List<String> pathsToHandleList;
    private static File excludeSourcePathsListFile;
    private static List<String> excludeSourcePathsList;

    private static File sourcePathFile;
    private static File destinationPathFile;

    private static boolean renameFiles;
    private static boolean deleteEmptyDirs;
    private static boolean deleteCopiedFiles;
    private static boolean ignoreExcludedPaths;
    private static boolean forceOverwrite;
    private static boolean disableRecursion;

    private static String getPathsListFile() {
        return argsParser.getPairArg(argsParser.findArgWithIndex(0, true));
    }

    private static String getExcludeSourcePathsListFile() {
        return argsParser.getPairArg(argsParser.findArgWithIndex(7, true));
    }

    private static String getSourcePath() {
        return argsParser.getPairArg(argsParser.findArgWithIndex(1, true));
    }

    private static String getDestinationPath() {
        return argsParser.getPairArg(argsParser.findArgWithIndex(2, true));
    }

    private static boolean renameFiles() {
        return argsParser.containsArg(3, true);
    }

    private static boolean deleteEmptyDirs() {
        return argsParser.containsArg(4, true);
    }

    private static boolean deleteCopiedFiles() {
        return argsParser.containsArg(5, true);
    }

    private static boolean ignoreExcludedPaths() {
        return argsParser.containsArg(6, true);
    }

    public static boolean forceOverwrite() {
        return argsParser.containsArg(8, true);
    }

    public static boolean disableRecursion() {
        return argsParser.containsArg(9, true);
    }

    private static boolean isFileAllowed(File file, boolean isSource) {
        if (file == null) {
            return false;
        }
        if (!ignoreExcludedPaths) {
            String excluded = Predicate.Methods.find(Arrays.asList(excludedPaths), element -> element != null && CompareUtils.stringMatches(file.getAbsolutePath(), element, CompareUtils.MatchStringOption.CONTAINS_IGNORE_CASE.flag));
            if (!TextUtils.isEmpty(excluded)) {
                logger.e("Not messing with " + (isSource ? "source" : "destination") + " file/directory \"" + file + "\" (contains part \"" + excluded + "\"), skipping...");
                return false;
            }
        }
        if (isSource) {
            if (sourcePathFile == null) {
                throw new RuntimeException("sourcePathFile is not initialized");
            }
            String prefix = sourcePathFile.getAbsolutePath();
            String filePath = file.getAbsolutePath();
            if (!filePath.startsWith(prefix)) {
                throw new RuntimeException("Source file not starts with ");
            }
            final String part = TextUtils.trim(filePath.substring(prefix.length(), filePath.length()), CompareUtils.Condition.EQUAL, File.separatorChar, true, true);
            String restrictedRelativePath = Predicate.Methods.find(excludeSourcePathsList, element -> !TextUtils.isEmpty(element) && !element.equals(File.separator) && part.startsWith(element));
            if (!TextUtils.isEmpty(restrictedRelativePath)) {
                logger.e("Source file \"" + file + "\" is excluded by relative path \"" + restrictedRelativePath + "\" from list in \"" + excludeSourcePathsListFile.getName() + "\"");
                return false;
            }
        }
        return true;
    }

    private static boolean isDestinationDirAllowed(File sourceFile, File destinationDir) {
        if (destinationDir == null) {
            return false;
        }
        if (destinationDir.getParentFile() == null) {
            logger.e("Root of partition is not allowed (destination directory \"" + destinationDir + (sourceFile != null ? "\" and source file \"" + sourceFile + "\")" : "\""));
            return false;
        }
        return true;
    }

    private static boolean allowOverwrite(File destinationFile) {
        boolean result = true;
        if (FileHelper.isFileExists(destinationFile)) {
            if (!forceOverwrite) {
                logger.i("Destination file \"" + destinationFile + "\" exists. Overwrite? (y/n)");
                String answer = StreamUtils.readStringFromInputStream(System.in, 1, false);
                result = answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
            }
        }
        return result;
    }

    private static boolean isSourceFileHandled(Map<Pair<File, File>, Boolean> map, File sourceFile) {
        return map != null && Predicate.Methods.contains(map.keySet(), element -> element != null && CompareUtils.objectsEqual(element.first, sourceFile));
    }

    public static void main(String args[]) {

        logger.i(System.getProperty(lineSeparator) + CopyUtil.class.getSimpleName() + ", version: 1.0.3.1"); // FIXME

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Args not specified!");
        }

        argsParser = new ArgsParser(argsNames);
        argsParser.setArgs(args);

        final String pathsToHandleListStr = TextUtils.trim(getPathsListFile(), false, true);
        if (!TextUtils.isEmpty(pathsToHandleListStr)) {
            FileHelper.checkFile(pathsToHandleListStr, false);
            pathsToHandleListFile = new File(pathsToHandleListStr);
        } else {
            pathsToHandleListFile = null;
        }

        final String excludeSourcePathsListStr = TextUtils.trim(getExcludeSourcePathsListFile(), false, true);
        if (!TextUtils.isEmpty(excludeSourcePathsListStr)) {
            FileHelper.checkFile(excludeSourcePathsListStr, false);
            excludeSourcePathsListFile = new File(excludeSourcePathsListStr);
        } else {
            excludeSourcePathsListFile = null;
        }

        final String sourcePath = TextUtils.trim(getSourcePath(), false, true);
        FileHelper.checkDir(sourcePath, false);
        sourcePathFile = new File(sourcePath);
        logger.i("Source path to copy/move from: \"" + sourcePathFile + "\"");

        final String destinationPath = TextUtils.trim(getDestinationPath(), false, true);
        if (TextUtils.isEmpty(destinationPath)) {
            throw new IllegalArgumentException("Destination path is not specified");
        }
        // not trying to create target dir because it may be file
        destinationPathFile = new File(destinationPath);
        logger.i("Destination path to copy/move to: \"" + destinationPathFile + "\"");

        if (sourcePathFile.equals(destinationPathFile)) {
            throw new IllegalArgumentException("Source path \"" + sourcePathFile + "\" is same as destination path \"" + destinationPathFile + "\"");
        }

        renameFiles = renameFiles();
        deleteEmptyDirs = deleteEmptyDirs();
        deleteCopiedFiles = deleteCopiedFiles();
        ignoreExcludedPaths = ignoreExcludedPaths();
        forceOverwrite = forceOverwrite();
        disableRecursion = disableRecursion();

        Set<Integer> unhandledIndexes = argsParser.getUnhandledArgsIndexes();
        for (Integer index : unhandledIndexes) {
            logger.e("Unknown argument \"" + args[index] + "\" (position: " + index + ")");
        }

        final Map<Pair<File, File>, Boolean> resultMap = new LinkedHashMap<>();

        if (pathsToHandleListFile != null) {
            pathsToHandleList = FileHelper.readStringsFromFile(pathsToHandleListFile);
        } else {
            pathsToHandleList = new ArrayList<>();
        }
        if (pathsToHandleList.isEmpty()) {
            pathsToHandleList.add(File.separator);
        }
        logger.i("Relative paths to copy/move:" + System.getProperty(lineSeparator) + pathsToHandleList);

        if (excludeSourcePathsListFile != null) {
            excludeSourcePathsList = FileHelper.readStringsFromFile(excludeSourcePathsListFile);
        } else {
            excludeSourcePathsList = new ArrayList<>();
        }
        if (!excludeSourcePathsList.isEmpty()) {
            logger.i("Relative paths to exclude from copy/move: " + System.getProperty(lineSeparator) + excludeSourcePathsList + System.getProperty(lineSeparator));
        }

        long startTime = System.nanoTime();

        for (String relativePath : pathsToHandleList) {

            relativePath = relativePath != null ? TextUtils.trim(relativePath, false, true) : null;

            if (TextUtils.isEmpty(relativePath)) {
                continue;
            }

            relativePath = TextUtils.trim(relativePath, false, true);

            final File sourcePathToHandle = !relativePath.equals(File.separator) ? new File(sourcePathFile, relativePath) : sourcePathFile;

            if (FileHelper.isFileExists(sourcePathToHandle)) {

                if (!isSourceFileHandled(resultMap, sourcePathToHandle)) {

                    boolean result = false;

                    File targetFile = null;

                    try {

                        targetFile = !relativePath.equals(File.separator) ? new File(destinationPathFile, relativePath) : new File(destinationPathFile, sourcePathToHandle.getName());

                        if (!isFileAllowed(sourcePathToHandle, true)) {
                            continue;
                        }

                        if (!isFileAllowed(targetFile, false)
                                || !isDestinationDirAllowed(sourcePathToHandle, targetFile.getParentFile())) {
                            continue;
                        }

                        boolean tryToCopy = true;

                        if (renameFiles) {
                            tryToCopy = false;
                            logger.i("Renaming \"" + sourcePathToHandle + "\" to \"" + targetFile + "\"...");
                            if (allowOverwrite(targetFile)) {
                                if (FileHelper.renameTo(sourcePathToHandle, targetFile.getParent(), targetFile.getName(), true, deleteEmptyDirs) != null) {
                                    logger.i("File \"" + sourcePathToHandle + "\" renamed successfully to \"" + targetFile + "\"");
                                    result = true;
                                } else {
                                    tryToCopy = true;
                                    logger.i("File \"" + sourcePathToHandle + "\" rename failed to " + targetFile + "\"");
                                }
                            }
                        }

                        if (!result && tryToCopy) {

                            result = true;

                            logger.i("Copying file \"" + sourcePathToHandle + "\" to \"" + targetFile + "\"...");
                            if (allowOverwrite(targetFile) && FileHelper.copyFileWithBuffering(sourcePathToHandle, targetFile.getName(), targetFile.getParent(), true, true, null) != null) {
                                logger.i("File \"" + sourcePathToHandle + "\" copied successfully to " + targetFile);
                                if (deleteCopiedFiles) {
                                    logger.i("Deleting copied file \"" + sourcePathToHandle + "\"...");
                                    if (!FileHelper.deleteFile(sourcePathToHandle)) {
                                        logger.e("Delete copied file \"" + sourcePathToHandle + "\" failed!");
                                    }
                                }
                            } else {
                                result = false;
                                logger.e("File \"" + sourcePathToHandle + "\" copy failed to \"" + targetFile + "\" !");
                            }
                        }
                    } finally {
                        resultMap.put(new Pair<>(sourcePathToHandle, targetFile), result);
                    }
                }

            } else if (FileHelper.isDirExists(sourcePathToHandle)) {

                boolean isAllowed = true;

                if (sourcePathToHandle.getParentFile() == null) {
                    logger.i("Source directory \"" + sourcePathToHandle + "\" to copy/move from is root of the partition. Proceed? (y/n)");
                    String answer = StreamUtils.readStringFromInputStream(System.in, 1, false);
                    isAllowed = answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
                }

                if (isAllowed) {

                    if (renameFiles) {

                        Set<File> filesToRename = FileHelper.getFiles(sourcePathToHandle, FileHelper.GetMode.FILES, null, null, disableRecursion ? 0 : FileHelper.DEPTH_UNLIMITED);
                        for (File f : filesToRename) {

                            if (!isSourceFileHandled(resultMap, f)) {

                                boolean tryToCopy = false;

                                boolean result = false;

                                File targetFile = null;

                                try {

                                    if (!isFileAllowed(f, true)) {
                                        continue;
                                    }

                                    String part = f.getParent();

                                    if (part.startsWith(sourcePathFile.getAbsolutePath())) {
                                        part = part.substring(sourcePathFile.getAbsolutePath().length(), part.length()); // remove source path prefix from target file
                                    }

                                    File targetDir = !TextUtils.isEmpty(part) ? new File(destinationPathFile, part) : destinationPathFile;
                                    targetFile = new File(targetDir, f.getName());

                                    if (!isFileAllowed(targetFile, false)
                                            || !isDestinationDirAllowed(f, targetDir)) {
                                        continue;
                                    }

                                    logger.i("Renaming \"" + f + "\" to \"" + targetFile + "\"...");
                                    if (allowOverwrite(targetFile)) {
                                        if (FileHelper.renameTo(f, targetFile.getParent(), targetFile.getName(), true, deleteEmptyDirs) != null) {
                                            logger.i("File \"" + f + "\" renamed successfully to \"" + targetFile + "\"");
                                            result = true;
                                        } else {
                                            tryToCopy = true;
                                            logger.e("File \"" + f + "\" rename failed to \"" + targetFile + "\"");
                                        }
                                    }

                                    if (!result && tryToCopy) {

                                        result = true;

                                        logger.i("Copying file \"" + f + "\" to \"" + targetFile + "\"...");
                                        if (allowOverwrite(targetFile) && FileHelper.copyFileWithBuffering(f, targetFile.getName(), targetFile.getParent(), true, true, null) != null) {
                                            logger.i("File \"" + f + "\" copied successfully to " + targetFile);
                                            if (deleteCopiedFiles) {
                                                logger.i("Deleting copied file \"" + f + "\"...");
                                                if (!FileHelper.deleteFile(f)) {
                                                    logger.e("Delete copied file \"" + f + "\" failed!");
                                                }
                                            }
                                        } else {
                                            result = false;
                                            logger.e("File \"" + f + "\" copy failed to \"" + targetFile + "\" !");
                                        }
                                    }

                                } finally {
                                    resultMap.put(new Pair<>(f, targetFile), result);
                                }
                            }
                        }

                    } else {
                        final File targetDir = !relativePath.equals(File.separator) ? new File(destinationPathFile, relativePath) : destinationPathFile;
                        FileHelper.copyFilesWithBuffering2(sourcePathToHandle, targetDir, null, null, new FileHelper.IMultipleCopyNotifier2() {
                            @Override
                            public boolean onCalculatingSize(File current, Set<File> collected) {
                                return !Thread.currentThread().isInterrupted();
                            }

                            @Override
                            public boolean onProcessing(File currentFile, File destDir, Set<File> copied, long filesProcessed, long filesTotal) {
                                return !Thread.currentThread().isInterrupted();
                            }

                            @Override
                            public boolean confirmCopy(File currentFile, File destDir) {
                                final boolean isConfirmed = !isSourceFileHandled(resultMap, currentFile)
                                        && isFileAllowed(currentFile, true)
                                        && isFileAllowed(destDir, false)
                                        && isDestinationDirAllowed(currentFile, destDir);
                                if (!isConfirmed) {
                                    resultMap.put(new Pair<>(currentFile, new File(destDir, currentFile.getName())), false);
                                }
                                return isConfirmed;
                            }

                            @Override
                            public File onBeforeCopy(File currentFile, File destDir) {
                                logger.i("Copying file \"" + currentFile + "\" to dir \"" + destDir + "\"...");
                                return null;
                            }

                            @Override
                            public boolean onExists(File destFile) {
                                return allowOverwrite(destFile);
                            }

                            @Override
                            public void onSucceeded(File currentFile, File resultFile) {
                                logger.i("File \"" + currentFile + "\" copied successfully to \"" + resultFile + "\"");
                                if (deleteCopiedFiles) {
                                    logger.i("Deleting copied \"" + currentFile + "\"...");
                                    if (!FileHelper.deleteFile(currentFile)) {
                                        logger.e("Delete copied file \"" + currentFile + "\" failed!");
                                    }
                                }
                                resultMap.put(new Pair<>(currentFile, resultFile), true);
                            }

                            @Override
                            public void onFailed(File currentFile, File destDir) {
                                logger.e("File \"" + currentFile + "\" copy failed to dir \"" + destDir + "\" !");
                                resultMap.put(new Pair<>(currentFile, new File(destDir.getParentFile(), currentFile.getName())), false);
                            }
                        }, true, disableRecursion ? 0 : FileHelper.DEPTH_UNLIMITED, null);

                        if (deleteEmptyDirs) {
                            FileHelper.deleteEmptyDir(sourcePathToHandle);
                        }
                    }
                }
            } else {
                logger.wtf("Incorrect source path: \"" + sourcePathToHandle + "\"");
                resultMap.put(new Pair<>(sourcePathToHandle, null), false);
            }
        }

        final long execTime = System.nanoTime() - startTime;

        final List<Map.Entry<Pair<File, File>, Boolean>> succeededFiles = Predicate.Methods.filter(resultMap.entrySet(), Map.Entry::getValue);
        final List<Map.Entry<Pair<File, File>, Boolean>> failedFiles = Predicate.Methods.filter(resultMap.entrySet(), element -> !element.getValue());
        logger.i("");
        if (!succeededFiles.isEmpty()) {
            logger.i("[--------------succeeded--------------]");
            logger.i(FileHelper.filePairsToString(Predicate.Methods.entriesToKeys(succeededFiles), 0));
        }
        if (!failedFiles.isEmpty()) {
            logger.i("[---------------failed----------------]");
            logger.i(FileHelper.filePairsToString(Predicate.Methods.entriesToKeys(failedFiles), 0));
        }
        logger.i("=======================================");
        logger.i("Copy/move done; succeeded: " + succeededFiles.size() + ", failed: " + failedFiles.size() + ", elapsed time: " + timeToString(execTime, TimeUnit.NANOSECONDS));
    }
}
