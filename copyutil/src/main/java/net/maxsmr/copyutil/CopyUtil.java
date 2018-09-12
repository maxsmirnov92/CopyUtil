package net.maxsmr.copyutil;

import net.maxsmr.copyutil.utils.CompareUtils;
import net.maxsmr.copyutil.utils.FileHelper;
import net.maxsmr.copyutil.utils.Pair;
import net.maxsmr.copyutil.utils.Predicate;
import net.maxsmr.copyutil.utils.TextUtils;
import net.maxsmr.copyutil.utils.logger.BaseLogger;
import net.maxsmr.copyutil.utils.logger.SimpleSystemLogger;
import net.maxsmr.copyutil.utils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static Pair<Integer, String> findArg(String[] argsNames, String[] args, int index) {
        if (argsNames == null || argsNames.length == 0) {
            throw new IllegalArgumentException("Incorrect args names: " + Arrays.toString(argsNames));
        }
        if (index < 0 || index >= argsNames.length) {
            throw new IllegalArgumentException("Incorrect arg name index: " + index);
        }
        return args != null?
                Predicate.Methods.findWithIndex(Arrays.asList(args), element -> element != null && element.equals(argsNames[index]))
                : null;
    }

    public static String getPairArg(String args[], Pair<Integer, String> pair) {
        return args != null && pair != null && pair.first < args.length - 1? args[pair.first + 1] : null;
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
                logger.e("Not messing with " + (isSource? "source" : "destination") + " file \"" + file + "\" (contains \"" + excluded + "\"), skipping...");
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]) {

        logger.i(System.getProperty("line.separator"));

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Args not specified!");
        }

        final String pathsList = getPathsListFile(args);
        FileHelper.checkFile(pathsList, false);
        final File pathsListFile = new File(pathsList);

        final String sourcePath = getSourcePath(args);
        FileHelper.checkDir(sourcePath, false);
        final File sourcePathFile = new File(sourcePath);
        logger.i("Source path to copy/move from: \"" + sourcePathFile + "\"");

        final String destinationPath = getDestinationPath(args);
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

        List<String> pathsToCopyList = FileHelper.readStringsFromFile(pathsListFile);
        logger.i("Paths to copy/move:" + System.getProperty("line.separator") + pathsToCopyList + System.getProperty("line.separator"));

        for (String relativePath : pathsToCopyList) {

            if (TextUtils.isEmpty(relativePath)) {
                continue;
            }

            final File sourcePathToCopy = !relativePath.equals(File.separator)? new File(sourcePathFile, relativePath) : sourcePathFile;

            if (FileHelper.isFileExists(sourcePathToCopy)) {

                if (!resultMap.containsKey(sourcePathToCopy)) {

                    if (!isFileAllowed(allowIgnoreExcludedPaths, sourcePathToCopy, true)) {
                        continue;
                    }

                    final File targetFile = !relativePath.equals(File.separator)? new File(destinationPathFile, relativePath) : new File(destinationPathFile, sourcePathToCopy.getName());

                    if (!isFileAllowed(allowIgnoreExcludedPaths, targetFile, false)) {
                        continue;
                    }

                    boolean result = true;

                    boolean tryToCopy = true;

                    if (allowRenameFiles) {
                        logger.i("Renaming \"" + sourcePathToCopy + "\" to \"" + targetFile + "\"...");
                        if (FileHelper.renameTo(sourcePathToCopy, targetFile.getParent(), targetFile.getName(), true, allowDeleteEmptyDirs) != null) {
                            logger.i("File \"" + sourcePathToCopy + "\" renamed successfully to \"" + targetFile + "\"");
                            tryToCopy = false;
                        } else {
                            logger.i("File \"" + sourcePathToCopy + "\" rename failed to " + targetFile + "\"");
                        }
                    }

                    if (tryToCopy) {

                        result = false;

                        logger.i("Copying file \"" + sourcePathToCopy + "\" to \"" + targetFile + "\"...");
                        if (FileHelper.copyFileWithBuffering(sourcePathToCopy, targetFile.getName(), targetFile.getParent(), true, true, null) != null) {
                            logger.i("File \"" + sourcePathToCopy + "\" copied successfully to " + targetFile);
                            if (allowDeleteCopiedFiles) {
                                logger.i("Deleting copied file \"" + sourcePathToCopy + "\"...");
                                if (!FileHelper.deleteFile(sourcePathToCopy)) {
                                    logger.e("Delete copied file \"" + sourcePathToCopy + "\" failed!");
                                }
                            }
                            result = true;
                        } else {
                            logger.e("File \"" + sourcePathToCopy + "\" copy failed to \"" + targetFile + "\" !");
                        }
                    }

                    resultMap.put(tryToCopy? sourcePathToCopy : targetFile, result);
                }

            } else if (FileHelper.isDirExists(sourcePathToCopy)) {

                boolean tryToCopy = true;

                if (allowRenameFiles) {
                    Set<File> filesToRename = FileHelper.getFiles(sourcePathToCopy, FileHelper.GetMode.FILES, null, null, FileHelper.DEPTH_UNLIMITED);
                    for (File f : filesToRename) {

                        if (!resultMap.containsKey(f)) {

                            if (!isFileAllowed(allowIgnoreExcludedPaths, f, true)) {
                                continue;
                            }

                            File targetDir = new File(destinationPathFile, f.getParent().replaceFirst(TextUtils.appendOrReplaceChar(sourcePathFile.getAbsolutePath(), '\\', "\\", false, true), ""));
                            File targetFile = new File(targetDir, f.getName());

                            if (!isFileAllowed(allowIgnoreExcludedPaths, targetFile, false)) {
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

                        }
                    }
                }

                if (tryToCopy) {
                    final File targetDir = !relativePath.equals(File.separator)? new File(destinationPathFile, relativePath) : destinationPathFile;
                    boolean finalDeleteCopiedFiles = allowDeleteCopiedFiles;
                    FileHelper.copyFilesWithBuffering2(sourcePathToCopy, targetDir, null, null, new FileHelper.IMultipleCopyNotifier2() {
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
                            return !resultMap.containsKey(currentFile)
                                    && isFileAllowed(allowIgnoreExcludedPaths, currentFile, true) && isFileAllowed(allowIgnoreExcludedPaths, destinationPathFile, false);
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
                            if (finalDeleteCopiedFiles) {
                                logger.i("Deleting copied \"" + currentFile + "\"...");
                                if (!FileHelper.deleteFile(currentFile)) {
                                    logger.e("Delete copied file \"" + currentFile + "\" failed!");
                                }
                            }
                            resultMap.put(currentFile, true);
                        }

                        @Override
                        public void onFailed(File currentFile, File destFile) {
                            logger.e("File \"" + currentFile + "\" copy failed to dir \"" + destFile + "\" !");
                            resultMap.put(currentFile, false);
                        }
                    }, true, FileHelper.DEPTH_UNLIMITED, null);

                    if (allowDeleteEmptyDirs) {
                        FileHelper.deleteEmptyDir(sourcePathToCopy);
                    }
                }
            } else {
                logger.wtf("Incorrect source path: \"" + sourcePathToCopy + "\"");
                resultMap.put(sourcePathToCopy, false);
            }
        }

        logger.i("");
        logger.i("=======================================");
        logger.i(FileHelper.filesToString(Predicate.Methods.entriesToKeys(Predicate.Methods.filter(resultMap.entrySet(), Map.Entry::getValue)), 0));
        logger.i("=======================================");
        logger.i("Copy/move done; succeeded: " + Predicate.Methods.filter(resultMap.entrySet(), Map.Entry::getValue).size()
                + ", failed: " + Predicate.Methods.filter(resultMap.entrySet(), element -> !element.getValue()).size());
    }
}
