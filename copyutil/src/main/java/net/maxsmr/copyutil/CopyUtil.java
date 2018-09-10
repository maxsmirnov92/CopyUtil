package net.maxsmr.copyutil;

import net.maxsmr.copyutil.utils.FileHelper;
import net.maxsmr.copyutil.utils.TextUtils;
import net.maxsmr.copyutil.utils.logger.BaseLogger;
import net.maxsmr.copyutil.utils.logger.SimpleSystemLogger;
import net.maxsmr.copyutil.utils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CopyUtil {

    private static final BaseLogger logger;

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

    public static void main(String args[]) {

        if (args == null || args.length < 3) {
            throw new IllegalArgumentException("Incorrect args count: " + Arrays.toString(args));
        }

        FileHelper.checkFile(args[0], false);

        File pathsListFile = new File(args[0]);

        FileHelper.checkDir(args[1], false);

        File sourcePath = new File(args[1]);

        if (TextUtils.isEmpty(args[2])) {
            throw new IllegalArgumentException("Destination path is not specified");
        }

        File destinationPath = FileHelper.createNewDir(args[2]);

        if (destinationPath == null) {
            throw new RuntimeException("Can't create destination path");
        }

        boolean allowRenameFiles = false;

        if (args.length >= 4) {
            try {
                allowRenameFiles = Boolean.parseBoolean(args[3]);
            } catch (Exception ignored) {
            }
        }

        boolean allowDeleteEmptyDirs = false;

        if (args.length >= 5) {
            try {
                allowDeleteEmptyDirs = Boolean.parseBoolean(args[4]);
            } catch (Exception ignored) {
            }
        }

        boolean allowDeleteCopiedFiles = false;

        if (args.length >= 6) {
            try {
                allowDeleteCopiedFiles = Boolean.parseBoolean(args[5]);
            } catch (Exception ignored) {
            }
        }

        List<String> pathsToCopyList = FileHelper.readStringsFromFile(pathsListFile);

        for (String relativePath : pathsToCopyList) {
            if (TextUtils.isEmpty(relativePath)) {
                continue;
            }
            final File sourcePathToCopy = new File(sourcePath, relativePath);
            if (FileHelper.isFileExists(sourcePathToCopy)) {

                boolean tryToCopy = true;

                if (allowRenameFiles) {
                    logger.i("Renaming " + sourcePathToCopy + " to " + destinationPath.getAbsolutePath() + File.separator + relativePath + "...");
                    if (FileHelper.renameTo(sourcePathToCopy, destinationPath.getAbsolutePath(), relativePath, true, allowDeleteEmptyDirs) != null) {
                        logger.i("File " + sourcePathToCopy + " renamed successfully to " + destinationPath.getAbsolutePath() + File.separator + relativePath);
                        tryToCopy = false;
                    } else {
                        logger.i("File " + sourcePathToCopy + " rename failed to " + destinationPath.getAbsolutePath() + File.separator + relativePath);
                    }
                }

                if (tryToCopy) {
                    logger.i("Copying file " + sourcePathToCopy + " to " + destinationPath.getAbsolutePath() + File.separator + relativePath + "...");
                    if (FileHelper.copyFile(sourcePathToCopy, relativePath, destinationPath.getAbsolutePath(), true, true) != null) {
                        logger.i("File " + sourcePathToCopy + " copied successfully to " + destinationPath.getAbsolutePath() + File.separator + relativePath);
                        if (allowDeleteCopiedFiles) {
                            logger.i("Deleting copied file " + sourcePathToCopy + "...");
                            if (!FileHelper.deleteFile(sourcePathToCopy)) {
                                logger.e("Delete copied file " + sourcePathToCopy + " failed!");
                            }
                        }
                    } else {
                        logger.e("File " + sourcePathToCopy + " copy failed to " + destinationPath.getAbsolutePath() + File.separator + relativePath + " !");
                    }
                }


            } else if (FileHelper.isDirExists(sourcePathToCopy)) {

                boolean tryToCopy = true;

                if (allowRenameFiles) {
                    Set<File> filesToRename = FileHelper.getFiles(sourcePathToCopy, FileHelper.GetMode.FILES, null, null, FileHelper.DEPTH_UNLIMITED);
                    for (File f : filesToRename) {
                        String targetDir = new File(destinationPath, f.getParent().replaceFirst(TextUtils.changeString(sourcePath.getAbsolutePath(), '\\', "\\", true), "")).getAbsolutePath();
                        logger.i("Renaming " + f + " to " + targetDir + File.separator + f.getName() + "...");
                        if (FileHelper.renameTo(f, targetDir, f.getName(), true, allowDeleteEmptyDirs) != null) {
                            logger.i("File " + f + " renamed successfully to " + targetDir + File.separator + f.getName());
                            tryToCopy = false;
                        } else {
                            logger.e("File " + f + " rename failed to " + targetDir + File.separator + f.getName());
                        }
                    }
                }

                if (tryToCopy) {
                    boolean finalDeleteCopiedFiles = allowDeleteCopiedFiles;
                    FileHelper.copyFilesWithBuffering(sourcePathToCopy, new File(destinationPath, relativePath), null, null, new FileHelper.IMultipleCopyNotifier() {
                        @Override
                        public boolean onCalculatingSize(File current, Set<File> collected, int currentLevel) {
                            return true;
                        }

                        @Override
                        public boolean onProcessing(File currentFile, File destDir, Set<File> copied, long filesTotal, int currentLevel) {
                            logger.i("Copying file " + currentFile + " to dir " + destDir + "...");
                            return true;
                        }

                        @Override
                        public boolean confirmCopy(File currentFile, File destDir, int currentLevel) {
                            return true;
                        }

                        @Override
                        public File onBeforeCopy(File currentFile, File destDir, int currentLevel) {
                            return null;
                        }

                        @Override
                        public boolean onExists(File destFile, int currentLevel) {
                            return true;
                        }

                        @Override
                        public void onSucceeded(File currentFile, File resultFile, int currentLevel) {
                            logger.i("File " + currentFile + " copied successfully to " + resultFile);
                            if (finalDeleteCopiedFiles) {
                                logger.i("Deleting copied " + currentFile + "...");
                                if (!FileHelper.deleteFile(currentFile)) {
                                    logger.e("Delete copied file " + currentFile + " failed!");
                                }
                            }
                        }

                        @Override
                        public void onFailed(File currentFile, File destFile, int currentLevel) {
                            logger.e("File " + currentFile + " copy failed to dir " + destFile + " !");
                        }
                    }, true, FileHelper.DEPTH_UNLIMITED);

                    if (allowDeleteEmptyDirs) {
                        FileHelper.deleteEmptyDir(sourcePathToCopy);
                    }
                }
            } else {
                logger.wtf("incorrect source path: " + sourcePathToCopy);
            }
        }
    }
}
