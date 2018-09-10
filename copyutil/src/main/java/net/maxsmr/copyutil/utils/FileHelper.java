package net.maxsmr.copyutil.utils;

import net.maxsmr.copyutil.utils.logger.BaseLogger;
import net.maxsmr.copyutil.utils.logger.holder.BaseLoggerHolder;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// TODO merge methods
public final class FileHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(FileHelper.class);

    public final static int DEPTH_UNLIMITED = -1;

    private FileHelper() {
        throw new AssertionError("no instances.");
    }


    public static boolean isFileLocked(File f) {
        final FileLock l = lockFileChannel(f, false);
        try {
            return l == null;
        } finally {
            releaseLockNoThrow(l);
        }
    }

    public static float getPartitionTotalSpace(String path, SizeUnit unit) {
        if (isDirExists(path)) {
            try {
                return SizeUnit.convert(new File(path).getTotalSpace(), SizeUnit.BYTES, unit);
            } catch (SecurityException e) {
                logger.e("a SecurityException occurred during convert(): " + e.getMessage(), e);
            }
        }
        return 0;
    }

    public static float getPartitionFreeSpace(String path, SizeUnit unit) {
        if (isDirExists(path)) {
            try {
                return SizeUnit.convert(new File(path).getFreeSpace(), SizeUnit.BYTES, unit);
            } catch (SecurityException e) {
                logger.e("a SecurityException occurred during convert(): " + e.getMessage(), e);
            }
        }
        return 0;
    }

    public static boolean isSizeCorrect(File file) {
        return (file != null && file.length() > 0);
    }

    public static boolean isFileCorrect(File file) {
        return (file != null && file.isFile() && isSizeCorrect(file));
    }

    public static boolean isFileExists(String fileName, String parentPath) {

        if (TextUtils.isEmpty(fileName) || fileName.contains("/")) {
            return false;
        }

        if (TextUtils.isEmpty(parentPath)) {
            return false;
        }

        File f = new File(parentPath, fileName);
        return f.exists() && f.isFile();
    }

    public static boolean isFileExists(File file) {
        return file != null && isFileExists(file.getAbsolutePath());
    }

    public static boolean isFileExists(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File f = new File(filePath);
            return (f.exists() && f.isFile());
        }
        return false;
    }


    public static String getCanonicalPath(File file) {
        if (file != null) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                logger.e("an IOException occurred during getCanonicalPath(): " + e.getMessage(), e);
                return file.getAbsolutePath();
            }
        }
        return null;
    }


    public static FileLock lockFileChannel(File f, boolean blocking) {

        if (f == null || !f.isFile() || !f.exists()) {
            logger.e("incorrect file: " + f);
            return null;
        }

        RandomAccessFile randomAccFile = null;
        FileChannel channel = null;

        try {
            randomAccFile = new RandomAccessFile(f, "rw");
            channel = randomAccFile.getChannel();

            try {
                return !blocking ? channel.tryLock() : channel.lock();

            } catch (IOException e) {
                logger.e("an IOException occurred during tryLock()", e);
            } catch (OverlappingFileLockException e) {
                logger.e("an OverlappingFileLockException occurred during tryLock()", e);
            }

        } catch (FileNotFoundException e) {
            logger.e("a FileNotFoundException occurred during new RandomAccessFile()", e);

        } finally {
            try {
                if (channel != null)
                    channel.close();
                if (randomAccFile != null)
                    randomAccFile.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }

        return null;
    }

    public static boolean releaseLockNoThrow(FileLock lock) {
        try {
            if (lock != null) {
                lock.release();
                return true;
            }
        } catch (IOException e) {
            logger.e("an IOException occurred during release()", e);
        }
        return false;
    }


    public static boolean isDirExists(String dirPath) {

        if (dirPath == null || dirPath.length() == 0) {
            return false;
        }

        return isDirExists(new File(dirPath));
    }

    public static boolean isDirExists(File dir) {
        return dir != null && dir.exists() && dir.isDirectory();
    }

    // TODO merge
    public static boolean checkFileNoThrow(File file, boolean createIfNotExists) {
        return file != null && (file.exists() && file.isFile() || (createIfNotExists && createNewFile(file.getName(), file.getParent()) != null));
    }

    public static void checkFile(File file, boolean createIfNotExists) {
        if (!checkFileNoThrow(file, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static boolean checkFileNoThrow(String file, boolean createIfNotExists) {
        return !TextUtils.isEmpty(file) && checkFileNoThrow(new File(file), createIfNotExists);
    }

    public static void checkFile(String file, boolean createIfNotExists) {
        if (!checkFileNoThrow(file, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static boolean checkDirNoThrow(String dirPath, boolean createIfNotExists) {
        if (!isDirExists(dirPath)) {
            if (!createIfNotExists) {
                return false;
            }
            if (createNewDir(dirPath) == null) {
                return false;
            }
        }
        return true;
    }

    public static void checkDir(String dirPath, boolean createIfNotExists) {
        if (!checkDirNoThrow(dirPath, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect directory path: " + dirPath);
        }
    }

    public static File checkPathNoThrow(String parent, String fileName, boolean createIfNotExists) {
        if (checkDirNoThrow(parent, createIfNotExists)) {
            if (!TextUtils.isEmpty(fileName)) {
                File f = new File(parent, fileName);
                if (checkFileNoThrow(f, createIfNotExists)) {
                    return f;
                }
            }
        }
        return null;
    }

    public static File checkPath(String parent, String fileName, boolean createIfNotExists) {
        File f = checkPathNoThrow(parent, fileName, createIfNotExists);
        if (f == null) {
            throw new IllegalArgumentException("incorrect path: " + parent + File.separator + fileName);
        }
        return f;
    }


    private static File createFile(String fileName, String parentPath, boolean recreate) {
        final File file;

        if (recreate) {
            file = createNewFile(fileName, parentPath);
        } else {
            if (!isFileExists(fileName, parentPath))
                file = createNewFile(fileName, parentPath);
            else
                file = new File(parentPath, fileName);
        }

        if (file == null) {
            logger.e("can't create file: " + parentPath + File.separator + fileName);
        }

        return file;
    }


    public static File createNewDir(String dirPath) {

        if (TextUtils.isEmpty(dirPath)) {
            logger.e("path is empty");
            return null;
        }

        File dir = new File(dirPath);

        if (dir.isDirectory() && dir.exists())
            return dir;

        if (dir.mkdirs())
            return dir;

        return null;
    }

    public static File createNewFile(String fileName, String parentPath) {
        return createNewFile(fileName, parentPath, true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")

    public static File createNewFile(String fileName, String parentPath, boolean recreate) {

        if (TextUtils.isEmpty(fileName) || fileName.contains(File.separator)) {
            return null;
        }

        if (TextUtils.isEmpty(parentPath)) {
            return null;
        }

        File newFile = null;

        File parentDir = new File(parentPath);

        boolean created = false;
        try {
            created = parentDir.mkdirs();
        } catch (SecurityException e) {
            logger.e("an Exception occurred", e);
        }

        if (created || parentDir.exists() && parentDir.isDirectory()) {

            newFile = new File(parentDir, fileName);

            if (newFile.exists() && newFile.isFile()) {
                if (!recreate || !newFile.delete()) {
                    newFile = null;
                }
            }

            if (newFile != null) {
                try {
                    if (!newFile.createNewFile()) {
                        newFile = null;
                    }
                } catch (IOException e) {
                    logger.e("an Exception occurred", e);
                    return null;
                }
            }
        }

        return newFile;
    }

    // TODO move
    public static File renameTo(File sourceFile, String destinationDir, String newFileName, boolean deleteIfExists, boolean deleteEmptyDirs) {

        if (!isFileExists(sourceFile)) {
            logger.e("Source file not exists: " + sourceFile);
            return null;
        }

        if (TextUtils.isEmpty(newFileName)) {
            logger.e("File name for new file is not specified");
            return null;
        }

        File newFile = null;

        File newDir = createNewDir(destinationDir);
        if (newDir != null) {
            newFile = new File(newDir, newFileName);

            if (isFileExists(newFile)) {
                logger.i("Target file " + newFile + " already exists");
                if (deleteIfExists) {
                    if (!deleteFile(newFile)) {
                        logger.e("Delete file " + newFile + " failed");
                        newFile = null;
                    }
                } else {
                    newFile = null;
                }
            }

            if (newFile != null) {
                logger.i("Renaming file " + sourceFile + " to " + newFile + "...");
                if (sourceFile.renameTo(newFile)) {
                    logger.i("File " + sourceFile + " renamed successfully to " + newFile);
                    File sourceParentDir = sourceFile.getParentFile();
                    if (deleteEmptyDirs) {
                        deleteEmptyDir(sourceParentDir);
                    }
                } else {
                    logger.e("File " + sourceFile + " rename failed to " + newFile);
                }
            }
        } else {
            logger.e("Create new dir: " + destinationDir + " failed");
        }

        return newFile;
    }

    // TODO move
    public static boolean isDirEmpty(File dir) {
        if (isDirExists(dir)) {
            File[] files = dir.listFiles();
            return files == null || files.length == 0;
        }
        return false;
    }

    public static boolean isBinaryFile(File f) throws FileNotFoundException, IOException {

        byte[] data = readBytesFromFile(f);

        if (data == null) {
            return false;
        }

        int ascii = 0;
        int other = 0;

        for (byte b : data) {
            if (b < 0x09)
                return true;

            if (b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D)
                ascii++;
            else if (b >= 0x20 && b <= 0x7E)
                ascii++;
            else
                other++;
        }

        return other != 0 && 100 * other / (ascii + other) > 95;
    }

    public static boolean revectorStream(InputStream in, OutputStream out) {
        return revectorStream(in, out, null);
    }

    public static boolean revectorStream(InputStream in, OutputStream out, IStreamNotifier notifier) {

        if (in == null || out == null)
            return false;

        boolean result = true;

        try {
            byte[] buff = new byte[256];

            int bytesWriteCount = 0;
            int totalBytesCount = 0;
            try {
                totalBytesCount = in.available();
            } catch (IOException e) {
                logger.e("an IOException occurred", e);
            }

            int len;
            long lastNotifyTime = 0;
            while ((len = in.read(buff)) > 0) {
                if (notifier != null) {
                    long interval = notifier.notifyInterval();
                    if (interval <= 0 || lastNotifyTime == 0 || (System.currentTimeMillis() - lastNotifyTime) >= interval) {
                        if (!notifier.onProcessing(in, out, bytesWriteCount,
                                totalBytesCount > 0 && bytesWriteCount <= totalBytesCount ? totalBytesCount - bytesWriteCount : 0)) {
                            result = false;
                            break;
                        }
                        lastNotifyTime = System.currentTimeMillis();
                    }

                }
                out.write(buff, 0, len);
                bytesWriteCount += len;
            }

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
            result = false;

        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }

        return result;
    }


    public static byte[] readBytesFromInputStream(InputStream inputStream) {

        if (inputStream != null) {

            try {

                byte[] data = new byte[inputStream.available()];
                int readByteCount;
                do {
                    readByteCount = inputStream.read(data, 0, data.length);
                } while (readByteCount > 0);

                return data;

            } catch (IOException e) {
                logger.e("an Exception occurred", e);

            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.e("an IOException occurred during close()", e);
                }
            }
        }

        return null;
    }


    public static byte[] readBytesFromFile(File file) {

        if (!isFileCorrect(file)) {
            logger.e("incorrect file: " + file);
            return null;
        }

        if (!file.canRead()) {
            logger.e("can't read from file: " + file);
            return null;
        }

        try {
            return readBytesFromInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.e("a FileNotFoundException occurred", e);
            return null;
        }
    }


    public static List<String> readStringsFromFile(File file) {

        List<String> lines = new ArrayList<>();

        if (!isFileCorrect(file)) {
            logger.e("incorrect file: " + file);
            return lines;
        }

        if (!file.canRead()) {
            logger.e("can't read from file: " + file);
            return lines;
        }

        try {
            return readStringsFromInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.e("an IOException occurred", e);
            return lines;
        }
    }


    public static String readStringFromFile(File file) {
        List<String> strings = readStringsFromFile(file);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings.toArray(new String[strings.size()])) : null;
    }


    public static List<String> readStringsFromInputStream(InputStream is) {
        if (is != null) {
            BufferedReader in = null;
            try {
                List<String> out = new ArrayList<>();
                in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    out.add(line);
                }
                return out;
            } catch (IOException e) {
                logger.e("an IOException occurred", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.e("an IOException occurred during close()", e);
                    }
                }
            }
        }
        return Collections.emptyList();
    }


    public static String readStringFromInputStream(InputStream is) {
        Collection<String> strings = readStringsFromInputStream(is);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings.toArray(new String[strings.size()])) : null;
    }

    public static String convertInputStreamToString(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        revectorStream(inputStream, result);
        try {
            return result.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.e("an Exception occurred", e);
            return null;
        }
    }

    public static boolean writeBytesToFile(File file, byte[] data, boolean append) {
        if (data == null || data.length == 0) {
            return false;
        }
        if (!isFileExists(file.getAbsolutePath()) && (file = createNewFile(file.getName(), file.getAbsolutePath(), !append)) == null) {
            return false;
        }
        if (!file.canWrite()) {
            logger.e("can't write to file: " + file);
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath(), append);
        } catch (FileNotFoundException e) {
            logger.e("an Exception occurred", e);
        }
        if (fos != null) {
            try {
                fos.write(data);
                fos.flush();
                return true;
            } catch (IOException e) {
                logger.e("an Exception occurred", e);
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.e("an Exception occurred", e);
                }
            }
        }
        return false;

    }


    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append) {
        return writeFromStreamToFile(data, fileName, parentPath, append, null);
    }


    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append, IStreamNotifier notifier) {
        logger.d("writeFromStreamToFile(), data=" + data + ", fileName=" + fileName + ", parentPath=" + parentPath + ", append=" + append);

        final File file = createFile(fileName, parentPath, !append);

        if (file == null) {
            return null;
        }

        if (!file.canWrite()) {
            return file;
        }

        try {
            if (revectorStream(data, new FileOutputStream(file), notifier)) {
                return file;
            }
        } catch (FileNotFoundException e) {
            logger.e("an Exception occurred", e);
        }

        return null;
    }

    public static boolean writeStringToFile(File file, String data, boolean append) {
        return writeStringsToFile(file, Collections.singletonList(data), append);
    }

    public static boolean writeStringsToFile(File file, Collection<String> data, boolean append) {

        if (data == null || data.isEmpty()) {
            return false;
        }

        if (file == null || !isFileExists(file.getAbsolutePath()) && (file = createNewFile(file.getName(), file.getAbsolutePath(), !append)) == null) {
            return false;
        }
        if (!file.canWrite()) {
            logger.e("can't write to file: " + file);
            return false;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            logger.d("an IOException occurred", e);
            return false;
        }

        BufferedWriter bw = new BufferedWriter(writer);

        try {
            for (String line : data) {
                bw.append(line);
                bw.append(System.getProperty("line.separator"));
                bw.flush();
            }
            return true;
        } catch (IOException e) {
            logger.e("an IOException occurred during write", e);
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }

        return false;
    }


    public static File compressFilesToZip(Collection<File> srcFiles, String destZipName, String destZipParent, boolean recreate) {

        if (srcFiles == null || srcFiles.isEmpty()) {
            logger.e("source files is null or empty");
            return null;
        }

        File zipFile = createFile(destZipName, destZipParent, recreate);

        if (FileHelper.isFileExists(zipFile)) {
            logger.e("cannot create zip file");
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(destZipName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            try {
                int zippedFiles = 0;

                for (File srcFile : new ArrayList<>(srcFiles)) {

                    if (!isFileCorrect(srcFile)) {
                        logger.e("incorrect file to zip: " + srcFile);
                        continue;
                    }

                    byte[] bytes = readBytesFromFile(srcFile);

                    ZipEntry entry = new ZipEntry(srcFile.getName());
                    zos.putNextEntry(entry);
                    if (bytes != null) {
                        zos.write(bytes);
                    }
                    zos.closeEntry();

                    zippedFiles++;
                }

                return zippedFiles > 0 ? new File(destZipName) : null;

            } catch (Exception e) {
                logger.e("an Exception occurred", e);

            } finally {

                try {
                    zos.close();
                    os.close();
                } catch (IOException e) {
                    logger.e("an IOException occurred during close()", e);
                }

            }

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
        }

        return null;
    }

    public static boolean unzipFile(File zipFile, File destPath, boolean saveDirHierarchy) {

        if (!isFileCorrect(zipFile)) {
            logger.e("incorrect zip file: " + zipFile);
            return false;
        }

        if (destPath == null) {
            logger.e("destPath is null");
            return false;
        }

        ZipFile zip = null;

        InputStream zis = null;
        OutputStream fos = null;

        try {
            zip = new ZipFile(zipFile);

            for (ZipEntry e : Collections.list(zip.entries())) {

                if (e.isDirectory() && !saveDirHierarchy) {
                    continue;
                }

                final String[] parts = e.getName().split(File.separator);
                final String entryName = !saveDirHierarchy && parts.length > 0 ? parts[parts.length - 1] : e.getName();

                final File path = new File(destPath, entryName);

                if (e.isDirectory()) {
                    if (!checkDirNoThrow(path.getAbsolutePath(), true)) {
                        logger.e("can't create directory: " + path);
                        return false;
                    }

                } else {
                    if (createNewFile(path.getName(), path.getParent()) == null) {
                        logger.e("can't create new file: " + path);
                        return false;
                    }

                    zis = zip.getInputStream(e);
                    fos = new FileOutputStream(path);

                    if (!revectorStream(zis, fos)) {
                        logger.e("revectorStream() failed");
                        return false;
                    }

                    zis.close();
                    fos.close();
                }
            }

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
            return false;

        } finally {

            try {
                if (zip != null) {
                    zip.close();
                }
                if (zis != null) {
                    zis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }

        return true;
    }

    public final static String[] IMAGES_EXTENSIONS = {"bmp", "jpg", "jpeg", "png"};

    public static boolean isPicture(String ext) {
        for (String pictureExt : IMAGES_EXTENSIONS) {
            if (pictureExt.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    public final static String[] VIDEO_EXTENSIONS = {"3gp", "mp4", "mov", "mpg"};

    public static boolean isVideo(String ext) {
        for (String videoExt : VIDEO_EXTENSIONS) {
            if (videoExt.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }


    public static String getFileExtension(String name) {
        int index = name.lastIndexOf('.');
        return (index > 0 && index < name.length() - 1) ? name.substring(index + 1) : "";
    }

    public static Set<File> getFiles(Collection<File> fromFiles, GetMode mode, Comparator<? super File> comparator, IGetNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        if (fromFiles != null) {
            for (File fromFile : fromFiles) {
                collected.addAll(getFiles(fromFile, mode, comparator, notifier, depth));
            }
        }
        return collected;
    }

    /**
     * @param fromFile file or directory
     * @return collected set of files or directories from specified directories without source files
     */

    public static Set<File> getFiles(File fromFile, GetMode mode, Comparator<? super File> comparator, IGetNotifier notifier, int depth) {
        return getFiles(fromFile, mode, comparator, notifier, depth, 0, null);
    }


    private static Set<File> getFiles(File fromFile, GetMode mode,
                                      Comparator<? super File> comparator, IGetNotifier notifier,
                                      int depth, int currentLevel, Set<File> collected) {

        final Set<File> result = new LinkedHashSet<>();

        if (collected == null) {
            collected = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(collected), currentLevel)) {
                    return result;
                }
            }

//            if (mode == GetMode.FOLDERS || mode == GetMode.ALL) {
//                if (notifier == null || notifier.onGet(fromFile)) {
//                    result.add(fromFile);
//                }
//            }

            boolean isCorrect = true;

            if (fromFile.isDirectory()) {

                File[] files = fromFile.listFiles();

                if (files != null) {

                    for (File f : files) {

                        if (f.isDirectory()) {

                            if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                result.addAll(getFiles(f, mode, comparator, notifier, depth, currentLevel + 1, collected));
                            }

                        } else if (f.isFile()) {
                            result.addAll(getFiles(f, mode, comparator, notifier, depth, currentLevel, collected));
                        } else {
                            logger.e("incorrect file or folder: " + f);
                        }
                    }
                }
            } else if (!fromFile.isFile()) {
                logger.e("incorrect file or folder: " + fromFile);
                isCorrect = false;
            }

            if (isCorrect) {
                if (fromFile.isFile() ? mode == GetMode.FILES : mode == GetMode.FOLDERS || mode == GetMode.ALL) {
                    if (notifier == null || (fromFile.isFile() ? notifier.onGetFile(fromFile) : notifier.onGetFolder(fromFile))) {
                        result.add(fromFile);
                        collected.add(fromFile);
                    }
                }
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }

        return result;
    }


    public static Set<File> getEnvPathFiles() {
        Set<File> searchFiles = new HashSet<>();
        String[] paths = System.getenv("PATH").split(":");
        if (paths.length == 0) {
            return Collections.emptySet();
        }
        for (String path : paths) {
            searchFiles.add(new File(path));
        }
        return searchFiles;
    }

    // TODO move
    public static boolean deleteEmptyDir(File dir) {
        return isDirEmpty(dir) && dir.delete();
    }

    public static boolean deleteFile(File file) {
        return file != null && file.isFile() && file.exists() && file.delete();
    }

    public static boolean deleteFile(String fileName, String parentPath) {
        if (isFileExists(fileName, parentPath)) {
            File f = new File(parentPath, fileName);
            return f.delete();
        }
        return false;
    }

    public static boolean deleteFile(String filePath) {
        if (isFileExists(filePath)) {
            File f = new File(filePath);
            return f.delete();
        }
        return false;
    }


    public static Set<File> delete(Collection<File> fromFiles, boolean deleteEmptyDirs, Collection<File> excludeFiles, Comparator<? super File> comparator, IDeleteNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        if (fromFiles != null) {
            for (File file : fromFiles) {
                collected.addAll(delete(file, deleteEmptyDirs, excludeFiles, comparator, notifier, depth));
            }
        }
        return collected;
    }

    /**
     * @param comparator to sort each folders list
     * @return set of deleted files
     */

    public static Set<File> delete(File fromFile, boolean deleteEmptyDirs, Collection<File> excludeFiles, Comparator<? super File> comparator, IDeleteNotifier notifier, int depth) {
        return delete(fromFile, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, 0, null);
    }


    private static Set<File> delete(File fromFile, boolean deleteEmptyDirs, Collection<File> excludeFiles, Comparator<? super File> comparator, IDeleteNotifier notifier,
                                    int depth, int currentLevel, Set<File> deletedFiles) {

        Set<File> result = new LinkedHashSet<>();

        if (deletedFiles == null) {
            deletedFiles = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(deletedFiles), currentLevel)) {
                    return result;
                }
            }

            if (fromFile.isDirectory()) {
                File[] files = fromFile.listFiles();

                if (files != null) {

                    if (comparator != null) {
                        List<File> sorted = new ArrayList<>(Arrays.asList(files));
                        Collections.sort(sorted, comparator);
                        files = sorted.toArray(new File[sorted.size()]);
                    }

                    for (File f : files) {

                        if (excludeFiles == null || !excludeFiles.contains(f)) {
                            if (f.isDirectory()) {
                                if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                    result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, currentLevel + 1, deletedFiles));
                                }
                                if (deleteEmptyDirs) {
                                    if (notifier == null || notifier.confirmDeleteFolder(f)) {
                                        if (f.delete()) {
                                            result.add(f);
                                            deletedFiles.add(f);
                                        } else if (notifier != null) {
                                            notifier.onDeleteFolderFailed(f);
                                        }
                                    }
                                }
                            } else if (f.isFile()) {
                                result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, currentLevel, deletedFiles));
                            } else {
                                logger.e("incorrect file or folder: " + f);
                            }
                        }
                    }

                    File[] remainFiles = fromFile.listFiles();

                    if (remainFiles == null || remainFiles.length == 0) {
                        if (deleteEmptyDirs) {
                            if (notifier == null || notifier.confirmDeleteFolder(fromFile)) {
                                if (fromFile.delete()) {
                                    result.add(fromFile);
                                    deletedFiles.add(fromFile);
                                } else if (notifier != null) {
                                    notifier.onDeleteFolderFailed(fromFile);
                                }
                            }
                        }
                    }
                }
            } else if (fromFile.isFile()) {

                if (notifier == null || notifier.confirmDeleteFile(fromFile)) {
                    if (fromFile.delete()) {
                        result.add(fromFile);
                        deletedFiles.add(fromFile);
                    } else if (notifier != null) {
                        notifier.onDeleteFileFailed(fromFile);
                    }
                }

            } else {
                logger.e("incorrect file or folder: " + fromFile);
            }
        }

        return result;
    }

    /**
     * This function will return size in form of bytes
     * реккурсивно подсчитывает размер папки в байтах
     *
     * @param f     файл или папка
     * @param depth глубина вложенности, 0 - текущий уровень
     */
    public static long getSize(File f, int depth) {
        return getSize(f, depth, 0);
    }


    private static long getSize(File f, int depth, int currentLevel) {
        long size = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                        size += getSize(file, depth, currentLevel + 1);
                    }
                }
            }
        } else if (f.isFile()) {
            size = f.length();
        }
        return size;
    }

    /**
     * @return dest file
     */
    public static File copyFile(File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate) {

        if (!isFileCorrect(sourceFile)) {
            logger.e("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.e("can't create dest file: " + destDir + File.separator + targetName);
            return null;
        }

        if (writeBytesToFile(destFile, readBytesFromFile(sourceFile), !rewrite)) {
            if (preserveFileDate) {
                destFile.setLastModified(sourceFile.lastModified());
            }
            return destFile;
        } else {
            logger.e("can't write to dest file: " + destDir + File.separator + targetName);
        }


        return null;
    }

    /**
     * @return dest file
     */
    public static File copyFileWithBuffering(final File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate,
                                             final ISingleCopyNotifier notifier) {

        if (!isFileExists(sourceFile)) {
            logger.e("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        final File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            return null;
        }

        final long totalBytesCount = sourceFile.length();

        try {
            if (writeFromStreamToFile(new FileInputStream(sourceFile), destFile.getName(), destFile.getParent(), !rewrite, notifier != null ? new IStreamNotifier() {
                @Override
                public long notifyInterval() {
                    return notifier.notifyInterval();
                }

                @Override
                public boolean onProcessing(InputStream inputStream, OutputStream outputStream, long bytesWrite, long bytesLeft) {
                    return notifier.onProcessing(sourceFile, destFile, bytesWrite, totalBytesCount);
                }
            } : null) != null) {
                if (preserveFileDate) {
                    destFile.setLastModified(sourceFile.lastModified());
                }
            }
        } catch (FileNotFoundException e) {
            logger.e("an Exception occurred", e);
        }

        return null;
    }

    /**
     * @param fromFile file or directory
     */

    public static Set<File> copyFilesWithBuffering(File fromFile, File destDir,
                                                   Comparator<? super File> comparator,
                                                   final ISingleCopyNotifier singleNotifier, final IMultipleCopyNotifier multipleCopyNotifier,
                                                   boolean preserveFileDate, int depth) {
        return copyFilesWithBuffering(fromFile, destDir, comparator, singleNotifier, multipleCopyNotifier, preserveFileDate, depth, 0, 0, null, null);
    }


    private static Set<File> copyFilesWithBuffering(File fromFile, File destDir,
                                                    Comparator<? super File> comparator,
                                                    final ISingleCopyNotifier singleNotifier, final IMultipleCopyNotifier multipleCopyNotifier,
                                                    boolean preserveFileDate, int depth,
                                                    int currentLevel, int totalFilesCount, Set<File> copied, List<String> exclusionList) {

        Set<File> result = new LinkedHashSet<>();

        if (copied == null) {
            copied = new LinkedHashSet<>();
        }

        boolean isCorrect = false;


        if (destDir != null) {

            if (fromFile != null && fromFile.exists()) {

                isCorrect = true;

                if (currentLevel == 0) {
                    totalFilesCount = getFiles(fromFile, GetMode.FILES, comparator, multipleCopyNotifier != null ? new IGetNotifier() {
                        @Override
                        public boolean onProcessing(File current, Set<File> collected, int currentLevel) {
                            return multipleCopyNotifier.onCalculatingSize(current, collected, currentLevel);
                        }

                        @Override
                        public boolean onGetFile(File file) {
                            return true;
                        }

                        @Override
                        public boolean onGetFolder(File folder) {
                            return false;
                        }
                    } : null, depth).size();


                    if (fromFile.isDirectory() && destDir.getAbsolutePath().startsWith(fromFile.getAbsolutePath())) {

                        File[] srcFiles = fromFile.listFiles();

                        if (srcFiles != null && srcFiles.length > 0) {
                            exclusionList = new ArrayList<>(srcFiles.length);
                            for (File srcFile : srcFiles) {
                                exclusionList.add(new File(destDir, srcFile.getName()).getAbsolutePath());
                            }
                        }
                    }

                }

                if (multipleCopyNotifier != null) {
                    if (!multipleCopyNotifier.onProcessing(fromFile, destDir, Collections.unmodifiableSet(copied), totalFilesCount, currentLevel)) {
                        return result;
                    }
                }

                if (fromFile.isDirectory()) {

                    File[] files = fromFile.listFiles();

                    if (files != null) {

                        if (comparator != null) {
                            List<File> sorted = new ArrayList<>(Arrays.asList(files));
                            Collections.sort(sorted, comparator);
                            files = sorted.toArray(new File[sorted.size()]);
                        }

                        for (File f : files) {

//                            if (currentLevel >= 1) {
//                                String tmpPath = destDir.getAbsolutePath();
//                                int index = tmpPath.lastIndexOf(File.separator);
//                                if (index > 0 && index < tmpPath.length() - 1) {
//                                    tmpPath = tmpPath.substring(0, index);
//                                }
//                                destDir = new File(tmpPath);
//                            }

                            if (f.isDirectory()) {
                                if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                    result.addAll(copyFilesWithBuffering(f, /*new File(destDir + File.separator + fromFile.getName(), f.getName())*/ destDir, comparator,
                                            singleNotifier, multipleCopyNotifier, preserveFileDate, depth, currentLevel + 1, totalFilesCount, copied, exclusionList));
                                }
                            } else {
                                result.addAll(copyFilesWithBuffering(f, /*new File(destDir, fromFile.getName()) */ destDir, comparator,
                                        singleNotifier, multipleCopyNotifier, preserveFileDate, depth, currentLevel, totalFilesCount, copied, exclusionList));
                            }
                        }

                    }

                    if (files == null || files.length == 0) {
                        String emptyDir = currentLevel == 0 ? destDir + File.separator + fromFile.getName() : destDir.getAbsolutePath();
                        if (!isDirExists(emptyDir)) {
                            createNewDir(emptyDir);
                        }
                    }
                } else if (isFileExists(fromFile)) {

                    File destFile = null;

                    boolean confirmCopy = true;

                    if (multipleCopyNotifier != null) {
                        confirmCopy = multipleCopyNotifier.confirmCopy(fromFile, destDir, currentLevel);
                    }

                    if (confirmCopy) {

                        if (multipleCopyNotifier != null) {
                            destFile = multipleCopyNotifier.onBeforeCopy(fromFile, destDir, currentLevel);
                        }

                        if (destFile == null) {
                            destFile = new File(destDir, fromFile.getName());
                        }

                        boolean rewrite = false;

                        if (multipleCopyNotifier != null && isFileExists(destFile)) {
                            rewrite = multipleCopyNotifier.onExists(destFile, currentLevel);
                        }

                        File resultFile = null;

                        if (exclusionList == null || !exclusionList.contains(fromFile.getAbsolutePath())) {
                            resultFile = copyFileWithBuffering(fromFile, destFile.getName(), destFile.getParent(), rewrite,
                                    preserveFileDate, singleNotifier);
                        }

                        if (resultFile != null) {
                            if (multipleCopyNotifier != null) {
                                multipleCopyNotifier.onSucceeded(fromFile, resultFile, currentLevel);
                            }
                            result.add(resultFile);
                            copied.add(resultFile);
                        } else {
                            isCorrect = false;
                        }
                    }
                } else {
                    isCorrect = false;
                }
            }

            if (!isCorrect) {
                logger.e("incorrect file or folder or failed to copy from: " + fromFile);
                if (multipleCopyNotifier != null) {
                    multipleCopyNotifier.onFailed(fromFile, destDir, currentLevel);
                }
            }
        } else {
            logger.e("destination dir is not specified");
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }
        return result;
    }

    public static boolean resetFile(File f) {
        if (f.isFile() && f.exists()) {

            if (f.length() == 0) {
                return true;
            }

            RandomAccessFile ra = null;
            try {
                ra = new RandomAccessFile(f, "rw");
                ra.setLength(0);
                return true;

            } catch (IOException e) {
                logger.e("an IOException occurred", e);
            } finally {
                if (ra != null) {
                    try {
                        ra.close();
                    } catch (IOException e) {
                        logger.e("an IOException occurred during close()", e);
                    }
                }
            }
        }
        return false;
    }


    public enum GetMode {
        FILES, FOLDERS, ALL
    }

    public interface IGetNotifier {

        /**
         * @return false if client code wants to interrupt collecting
         */
        boolean onProcessing(File current, Set<File> collected, int currentLevel);

        /**
         * @return false if client code doesn't want to append this file to result
         */
        boolean onGetFile(File file);


        /**
         * @return false if client code doesn't want to append this folder to result
         */
        boolean onGetFolder(File folder);
    }

    public interface IDeleteNotifier {

        /**
         * @return false if client code wants to interrupt deleting
         */
        boolean onProcessing(File current, Set<File> deleted, int currentLevel);

        /**
         * @return false if client code doesn't want to delete this file
         */
        boolean confirmDeleteFile(File file);

        /**
         * @return false if client code doesn't want to delete this folder
         */
        boolean confirmDeleteFolder(File folder);

        void onDeleteFileFailed(File file);

        void onDeleteFolderFailed(File folder);
    }

    public interface IStreamNotifier {

        long notifyInterval();

        boolean onProcessing(InputStream inputStream, OutputStream outputStream, long bytesWrite, long bytesLeft);
    }

    public interface ISingleCopyNotifier {

        long notifyInterval();

        boolean onProcessing(File sourceFile, File destFile, long bytesCopied, long bytesTotal);
    }

    public interface IMultipleCopyNotifier {

        boolean onCalculatingSize(File current, Set<File> collected, int currentLevel);

        boolean onProcessing(File currentFile, File destDir, Set<File> copied, long filesTotal, int currentLevel);

        boolean confirmCopy(File currentFile, File destDir, int currentLevel);

        /**
         * @return target file to copy in or null if default
         */
        File onBeforeCopy(File currentFile, File destDir, int currentLevel);

        boolean onExists(File destFile, int currentLevel);

        void onSucceeded(File currentFile, File resultFile, int currentLevel);

        void onFailed(File currentFile, File destDir, int currentLevel);
    }

    public static double sum(Collection<? extends Number> numbers) {
        double result = 0;
        if (numbers != null) {
            for (Number n : numbers) {
                if (n != null) {
                    result += n.doubleValue();
                }
            }
        }
        return result;
    }

    public enum SizeUnit {

        BYTES {
            @Override
            public long toBytes(float s) {
                return (long) s;
            }

            @Override
            public float toKBytes(float s) {
                return s / C1;
            }

            @Override
            public float toMBytes(float s) {
                return s / C2;
            }

            @Override
            public float toGBytes(float s) {
                return s / C3;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        KBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C1);
            }

            @Override
            public float toKBytes(float s) {
                return s;
            }

            @Override
            public float toMBytes(float s) {
                return s / C1;
            }

            @Override
            public float toGBytes(float s) {
                return s / C2;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        MBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C2);
            }

            @Override
            public float toKBytes(float s) {
                return s * C1;
            }

            @Override
            public float toMBytes(float s) {
                return s;
            }

            @Override
            public float toGBytes(float s) {
                return s / C1;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        GBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C3);
            }

            @Override
            public float toKBytes(float s) {
                return s * C2;
            }

            @Override
            public float toMBytes(float s) {
                return s * C1;
            }

            @Override
            public float toGBytes(float s) {
                return s;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        BITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) s;
            }

            @Override
            public float toKBits(float s) {
                return s / C1;
            }

            @Override
            public float toMBits(float s) {
                return s / C2;
            }

            @Override
            public float toGBits(float s) {
                return s / C3;
            }
        },

        KBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C1);
            }

            @Override
            public float toKBits(float s) {
                return s;
            }

            @Override
            public float toMBits(float s) {
                return s / C2;
            }

            @Override
            public float toGBits(float s) {
                return s / C3;
            }
        },

        MBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C2);
            }

            @Override
            public float toKBits(float s) {
                return s * C1;
            }

            @Override
            public float toMBits(float s) {
                return s;
            }

            @Override
            public float toGBits(float s) {
                return s / C1;
            }
        },

        GBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C3);
            }

            @Override
            public float toKBits(float s) {
                return s * C2;
            }

            @Override
            public float toMBits(float s) {
                return s * C1;
            }

            @Override
            public float toGBits(float s) {
                return s;
            }
        };

        public static final long C0 = 8;
        public static final long C1 = 1024L;
        public static final long C2 = C1 * 1024L;
        public static final long C3 = C2 * 1024L;

        public abstract long toBytes(float s);

        public abstract float toKBytes(float s);

        public abstract float toMBytes(float s);

        public abstract float toGBytes(float s);

        public abstract long toBits(float s);

        public abstract float toKBits(float s);

        public abstract float toMBits(float s);

        public abstract float toGBits(float s);

        public boolean isBits() {
            return this == BITS || this == KBITS || this == MBITS || this == GBITS;
        }

        public boolean isBytes() {
            return this == BYTES || this == KBYTES || this == MBYTES || this == GBYTES;
        }

        public static float toBitsFromBytes(float s) {
            return s * C0;
        }

        public static float toBytesFromBits(float s) {
            return s / C0;
        }

        public static float convert(long what, SizeUnit from, SizeUnit to) {
            final float result;
            switch (to) {
                case BITS:
                    result = from.toBits(what);
                    break;
                case BYTES:
                    result = from.toBytes(what);
                    break;
                case KBITS:
                    result = from.toKBits(what);
                    break;
                case KBYTES:
                    result = from.toKBytes(what);
                    break;
                case MBITS:
                    result = from.toMBits(what);
                    break;
                case MBYTES:
                    result = from.toMBytes(what);
                    break;
                case GBITS:
                    result = from.toGBits(what);
                    break;
                case GBYTES:
                    result = from.toGBytes(what);
                    break;
                default:
                    result = 0f;
                    break;
            }
            return result;
        }
    }


}
