package net.maxsmr.copyutil;

import net.maxsmr.copyutil.utils.CompareUtils;
import net.maxsmr.copyutil.utils.FileHelper;
import net.maxsmr.copyutil.utils.Predicate;
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

import static net.maxsmr.copyutil.utils.ArgsParser.findArg;
import static net.maxsmr.copyutil.utils.ArgsParser.getPairArg;

public class CopyUtil {

    // new File("") == jar working dir

    private static final BaseLogger logger;

    private static final String[] argsNames =
            {"-pathsListFile", "-sourcePath", "-destinationPath", "-renameFiles", "-deleteEmptyDirs", "-deleteCopiedFiles", "-ignoreExcludedPaths", "-excludeSourcePathsFile"};

    private static final String[] excludedPaths =
            {"Boot", "Documents and Settings", "ProgramData", "Program Files", "Program Files (x86)", "Recovery", "System Volume Information", "Windows", "Users"};

    static {
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

    private static File pathsToHandleListFile;
    private static List<String> pathsToHandleList;
    private static File excludeSourcePathsListFile;
    private static List<String> excludeSourcePathsList;

    private static File sourcePathFile;
    private static File destinationPathFile;

    private static boolean allowRenameFiles;
    private static boolean allowDeleteEmptyDirs;
    private static boolean allowDeleteCopiedFiles;
    private static boolean allowIgnoreExcludedPaths;

    public static String getPathsListFile(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 0, true));
    }

    public static String getSourcePath(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 1, true));
    }

    public static String getDestinationPath(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 2, true));
    }

    public static boolean allowRenameFiles(String args[]) {
        return findArg(argsNames, args, 3, true) != null;
    }

    public static boolean allowDeleteEmptyDirs(String args[]) {
        return findArg(argsNames, args, 4, true) != null;
    }

    public static boolean allowDeleteCopiedFiles(String args[]) {
        return findArg(argsNames, args, 5, true) != null;
    }

    public static boolean allowIgnoreExcludedPaths(String args[]) {
        return findArg(argsNames, args, 6, true) != null;
    }

    public static String getExcludeSourcePathsListFile(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 7, true));
    }

    public static boolean isFileAllowed(File file, boolean isSource) {
        if (file == null) {
            return false;
        }
        if (!allowIgnoreExcludedPaths) {
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
                logger.e("Source file \"" + file + "\" is excluded by relative path \"" + restrictedRelativePath + "\" from list");
                return false;
            }
        }
        return true;
    }

    public static boolean isDestinationDirAllowed(File sourceFile, File destinationDir) {
        if (destinationDir == null) {
            return false;
        }
        if (destinationDir.getParentFile() == null) {
            logger.e("Root of partition is not allowed (destination directory \"" + destinationDir + (sourceFile != null? "\" and source file \"" + sourceFile + "\")" : "\""));
            return false;
        }
        return true;
    }

    public static void main(String args[]) {

        logger.i(System.getProperty("line.separator"));

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Args not specified!");
        }

        final String pathsToHandleListStr = TextUtils.trim(getPathsListFile(args), false, true);
        if (!TextUtils.isEmpty(pathsToHandleListStr)) {
            FileHelper.checkFile(pathsToHandleListStr, false);
            pathsToHandleListFile = new File(pathsToHandleListStr);
        } else {
            pathsToHandleListFile = null;
        }

        final String excludeSourcePathsListStr = TextUtils.trim(getExcludeSourcePathsListFile(args), false, true);
        if (!TextUtils.isEmpty(excludeSourcePathsListStr)) {
            FileHelper.checkFile(excludeSourcePathsListStr, false);
            excludeSourcePathsListFile = new File(excludeSourcePathsListStr);
        } else {
            excludeSourcePathsListFile = null;
        }

        final String sourcePath = TextUtils.trim(getSourcePath(args), false, true);
        FileHelper.checkDir(sourcePath, false);
        sourcePathFile = new File(sourcePath);
        logger.i("Source path to copy/move from: \"" + sourcePathFile + "\"");

        final String destinationPath = TextUtils.trim(getDestinationPath(args), false, true);
        if (TextUtils.isEmpty(destinationPath)) {
            throw new IllegalArgumentException("Destination path is not specified");
        }
        // not trying to create target dir because it may be file
        destinationPathFile = new File(destinationPath);
        logger.i("Destination path to copy/move to: \"" + destinationPathFile + "\"");

        if (sourcePathFile.equals(destinationPathFile)) {
            throw new IllegalArgumentException("Source path \"" + sourcePathFile + "\" is same as destination path \"" + destinationPathFile + "\"");
        }

        allowRenameFiles = allowRenameFiles(args);
        allowDeleteEmptyDirs = allowDeleteEmptyDirs(args);
        allowDeleteCopiedFiles = allowDeleteCopiedFiles(args);
        allowIgnoreExcludedPaths = allowIgnoreExcludedPaths(args);

        final Map<File, Boolean> resultMap = new LinkedHashMap<>();

        if (pathsToHandleListFile != null) {
            pathsToHandleList = FileHelper.readStringsFromFile(pathsToHandleListFile);
        } else {
            pathsToHandleList = new ArrayList<>();
        }
        if (pathsToHandleList.isEmpty()) {
            pathsToHandleList.add(File.separator);
        }
        logger.i("Relative paths to copy/move:" + System.getProperty("line.separator") + pathsToHandleList + System.getProperty("line.separator"));

        if (excludeSourcePathsListFile != null) {
            excludeSourcePathsList = FileHelper.readStringsFromFile(excludeSourcePathsListFile);
        } else {
            excludeSourcePathsList = new ArrayList<>();
        }
        if (!excludeSourcePathsList.isEmpty()) {
            logger.i("Relative paths to exclude from copy/move: " + System.getProperty("line.separator") + excludeSourcePathsList + System.getProperty("line.separator"));
        }

        for (String relativePath : pathsToHandleList) {

            if (TextUtils.isEmpty(relativePath)) {
                continue;
            }

            relativePath = TextUtils.trim(relativePath, false, true);

            final File sourcePathToHandle = !relativePath.equals(File.separator) ? new File(sourcePathFile, relativePath) : sourcePathFile;

            if (FileHelper.isFileExists(sourcePathToHandle)) {

                if (!resultMap.containsKey(sourcePathToHandle)) {

                    boolean result = true;

                    File targetFile = null;

                    try {

                        if (!isFileAllowed(sourcePathToHandle, true)) {
                            result = false;
                            continue;
                        }

                        targetFile = !relativePath.equals(File.separator) ? new File(destinationPathFile, relativePath) : new File(destinationPathFile, sourcePathToHandle.getName());

                        if (!isFileAllowed(targetFile, false)
                                || !isDestinationDirAllowed(sourcePathToHandle, targetFile.getParentFile())) {
                            result = false;
                            continue;
                        }

                        if (allowRenameFiles) {
                            logger.i("Renaming \"" + sourcePathToHandle + "\" to \"" + targetFile + "\"...");
                            if (FileHelper.renameTo(sourcePathToHandle, targetFile.getParent(), targetFile.getName(), true, allowDeleteEmptyDirs) != null) {
                                logger.i("File \"" + sourcePathToHandle + "\" renamed successfully to \"" + targetFile + "\"");
                            } else {
                                result = false;
                                logger.i("File \"" + sourcePathToHandle + "\" rename failed to " + targetFile + "\"");
                            }
                        }

                        if (!result) {

                            result = true;

                            logger.i("Copying file \"" + sourcePathToHandle + "\" to \"" + targetFile + "\"...");
                            if (FileHelper.copyFileWithBuffering(sourcePathToHandle, targetFile.getName(), targetFile.getParent(), true, true, null) != null) {
                                logger.i("File \"" + sourcePathToHandle + "\" copied successfully to " + targetFile);
                                if (allowDeleteCopiedFiles) {
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
                        resultMap.put(result ? targetFile : sourcePathToHandle, result);
                    }
                }

            } else if (FileHelper.isDirExists(sourcePathToHandle)) {

                boolean tryToCopy = true;

                if (allowRenameFiles) {
                    Set<File> filesToRename = FileHelper.getFiles(sourcePathToHandle, FileHelper.GetMode.FILES, null, null, FileHelper.DEPTH_UNLIMITED);
                    for (File f : filesToRename) {

                        if (!resultMap.containsKey(f)) {

                            boolean isAllowed = true;

                            try {

                                if (!isFileAllowed(f, true)) {
                                    isAllowed = false;
                                    continue;
                                }

                                String part = f.getParent();

                                if (part.startsWith(sourcePathFile.getAbsolutePath())) {
                                    part = part.substring(sourcePathFile.getAbsolutePath().length(), part.length()); // remove source path prefix
                                }

                                File targetDir = !TextUtils.isEmpty(part)? new File(destinationPathFile, part) : destinationPathFile;
                                File targetFile = new File(targetDir, f.getName());

                                if (!isFileAllowed(targetFile, false)
                                        || !isDestinationDirAllowed(f, targetDir)) {
                                    isAllowed = false;
                                    continue;
                                }

                                logger.i("Renaming \"" + f + "\" to \"" + targetFile + "\"...");
                                if (FileHelper.renameTo(f, targetFile.getParent(), targetFile.getName(), true, allowDeleteEmptyDirs) != null) {
                                    logger.i("File \"" + f + "\" renamed successfully to \"" + targetFile + "\"");
                                    tryToCopy = false;
                                    resultMap.put(targetFile, true);
                                } else {
                                    logger.e("File \"" + f + "\" rename failed to \"" + targetFile + "\"");
                                }

                            } finally {
                                if (!isAllowed) {
                                    resultMap.put(f, false);
                                }
                            }
                        }
                    }
                }

                if (tryToCopy) {
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
                            final boolean isConfirmed = !resultMap.containsKey(currentFile)
                                    && isFileAllowed(currentFile, true)
                                    && isFileAllowed(destDir, false)
                                    && isDestinationDirAllowed(currentFile, destDir);
                            if (!isConfirmed) {
                                resultMap.put(currentFile, false);
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
                            return true;
                        }

                        @Override
                        public void onSucceeded(File currentFile, File resultFile) {
                            logger.i("File \"" + currentFile + "\" copied successfully to \"" + resultFile + "\"");
                            if (allowDeleteCopiedFiles) {
                                logger.i("Deleting copied \"" + currentFile + "\"...");
                                if (!FileHelper.deleteFile(currentFile)) {
                                    logger.e("Delete copied file \"" + currentFile + "\" failed!");
                                }
                            }
                            resultMap.put(resultFile, true);
                        }

                        @Override
                        public void onFailed(File currentFile, File destDir) {
                            logger.e("File \"" + currentFile + "\" copy failed to dir \"" + destDir + "\" !");
                            resultMap.put(currentFile, false);
                        }
                    }, true, FileHelper.DEPTH_UNLIMITED, null);

                    if (allowDeleteEmptyDirs) {
                        FileHelper.deleteEmptyDir(sourcePathToHandle);
                    }
                }
            } else {
                logger.wtf("Incorrect source path: \"" + sourcePathToHandle + "\"");
                resultMap.put(sourcePathToHandle, false);
            }
        }

        final List<Map.Entry<File, Boolean>> succeededFiles = Predicate.Methods.filter(resultMap.entrySet(), Map.Entry::getValue);
        final List<Map.Entry<File, Boolean>> failedFiles = Predicate.Methods.filter(resultMap.entrySet(), element -> !element.getValue());
        logger.i("");
        if (!succeededFiles.isEmpty()) {
            logger.i("[--------------succeeded--------------]");
            logger.i(FileHelper.filesToString(Predicate.Methods.entriesToKeys(succeededFiles), 0));
        }
        if (!failedFiles.isEmpty()) {
            logger.i("[---------------failed----------------]");
            logger.i(FileHelper.filesToString(Predicate.Methods.entriesToKeys(failedFiles), 0));
        }
        logger.i("=======================================");
        logger.i("Copy/move done; succeeded: " + succeededFiles.size() + ", failed: " + failedFiles.size());
    }
}
