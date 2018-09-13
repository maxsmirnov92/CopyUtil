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

// TODO exclusion source relative list
public class CopyUtil {

    private static final BaseLogger logger;

    private static final String[] argsNames =
            {"-pathsListFile", "-sourcePath", "-destinationPath", "-renameFiles", "-deleteEmptyDirs", "-deleteCopiedFiles", "-ignoreExcludedPaths"};

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

    public static String getPathsListFile(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 0));
    }

    public static String getSourcePath(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 1));
    }

    public static String getDestinationPath(String args[]) {
        return getPairArg(args, findArg(argsNames, args, 2));
    }

    public static boolean allowRenameFiles(String args[]) {
        return findArg(argsNames, args, 3) != null;
    }

    public static boolean allowDeleteEmptyDirs(String args[]) {
        return findArg(argsNames, args, 4) != null;
    }

    public static boolean allowDeleteCopiedFiles(String args[]) {
        return findArg(argsNames, args, 5) != null;
    }

    public static boolean allowIgnoreExcludedPaths(String args[]) {
        return findArg(argsNames, args, 6) != null;
    }

    public static boolean isFileAllowed(boolean allowIgnore, File file, boolean isSource) {
        if (file == null) {
            return false;
        }
        if (!allowIgnore) {
            String excluded = Predicate.Methods.find(Arrays.asList(excludedPaths), element -> element != null && CompareUtils.stringMatches(file.getAbsolutePath(), element, CompareUtils.MatchStringOption.CONTAINS_IGNORE_CASE.flag));
            if (!TextUtils.isEmpty(excluded)) {
                logger.e("Not messing with " + (isSource ? "source" : "destination") + " file/directory \"" + file + "\" (contains \"" + excluded + "\"), skipping...");
                return false;
            }
        }
        return true;
    }

    public static boolean isDestinationDirAllowed(File sourceFile, File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.getParentFile() == null) {
            logger.e("Root of partition is not allowed (target directory \"" + dir + (sourceFile != null? "\" and source file \"" + sourceFile + "\")" : "\""));
            return false;
        }
        return true;
    }

    public static void main(String args[]) {

        logger.i(System.getProperty("line.separator"));

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Args not specified!");
        }

        final String pathsList = TextUtils.trim(getPathsListFile(args), false, true);
        final File pathsListFile;
        if (!TextUtils.isEmpty(pathsList)) {
            FileHelper.checkFile(pathsList, false);
            pathsListFile = new File(pathsList);
        } else {
            pathsListFile = null;
        }

        final String sourcePath = TextUtils.trim(getSourcePath(args), false, true);
        FileHelper.checkDir(sourcePath, false);
        final File sourcePathFile = new File(sourcePath);
        logger.i("Source path to copy/move from: \"" + sourcePathFile + "\"");

        final String destinationPath = TextUtils.trim(getDestinationPath(args), false, true);
        if (TextUtils.isEmpty(destinationPath)) {
            throw new IllegalArgumentException("Destination path is not specified");
        }
        // not trying to create target dir because it may be file (for case when single separator in list)
        File destinationPathFile = new File(destinationPath);
        logger.i("Destination path to copy/move to: \"" + destinationPathFile + "\"");

        if (sourcePathFile.equals(destinationPathFile)) {
            throw new IllegalArgumentException("Source path \"" + sourcePathFile + "\" is same as destination path \"" + destinationPathFile + "\"");
        }

        final boolean allowRenameFiles = allowRenameFiles(args);
        final boolean allowDeleteEmptyDirs = allowDeleteEmptyDirs(args);
        final boolean allowDeleteCopiedFiles = allowDeleteCopiedFiles(args);
        final boolean allowIgnoreExcludedPaths = allowIgnoreExcludedPaths(args);

        final Map<File, Boolean> resultMap = new LinkedHashMap<>();

        final List<String> pathsToCopyList;
        if (pathsListFile != null) {
            pathsToCopyList = FileHelper.readStringsFromFile(pathsListFile);
        } else {
            pathsToCopyList = new ArrayList<>();
        }
        if (pathsToCopyList.isEmpty()) {
            pathsToCopyList.add(File.separator);
        }
        logger.i("Paths to copy/move:" + System.getProperty("line.separator") + pathsToCopyList + System.getProperty("line.separator"));

        for (String relativePath : pathsToCopyList) {

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

                        if (!isFileAllowed(allowIgnoreExcludedPaths, sourcePathToHandle, true)) {
                            result = false;
                            continue;
                        }

                        targetFile = !relativePath.equals(File.separator) ? new File(destinationPathFile, relativePath) : new File(destinationPathFile, sourcePathToHandle.getName());

                        if (!isFileAllowed(allowIgnoreExcludedPaths, targetFile, false)
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

                                if (!isFileAllowed(allowIgnoreExcludedPaths, f, true)) {
                                    isAllowed = false;
                                    continue;
                                }

                                File targetDir = new File(destinationPathFile, f.getParent().replaceFirst(TextUtils.appendOrReplaceChar(sourcePathFile.getAbsolutePath(), '\\', "\\", false, true), ""));
                                File targetFile = new File(targetDir, f.getName());

                                if (!isFileAllowed(allowIgnoreExcludedPaths, targetFile, false)
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
                                    && isFileAllowed(allowIgnoreExcludedPaths, currentFile, true)
                                    && isFileAllowed(allowIgnoreExcludedPaths, destDir, false)
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
            logger.i("[==============succeeded==============]");
            logger.i(FileHelper.filesToString(Predicate.Methods.entriesToKeys(succeededFiles), 0));
        }
        if (!failedFiles.isEmpty()) {
            logger.i("[===============failed================]");
            logger.i(FileHelper.filesToString(Predicate.Methods.entriesToKeys(failedFiles), 0));
        }
        logger.i("=======================================");
        logger.i("Copy/move done; succeeded: " + succeededFiles.size() + ", failed: " + failedFiles.size());
    }
}
