# CopyUtil
a small util to copy/move files or folders specified in list file from source directory to target directory

usage java -jar copyutil.jar -pathsListFile "" -sourcePath "" -destinationPath "" -renameFiles -deleteEmptyDirs -deleteCopiedFiles -ignoreExcludedPaths
-pathsListFile = absolute path to file with relative paths that should be copied/moved; can be relative path to file or directory or may consists of only one separator char - that means thar all root content of source directory should be copied or source file should be copied itself (if it is file of course); separated with new line character
-sourcePath = source root absolute path: where copy/move files from
-destinationPath = destination root absolute path: where copy/move files to
-renameFiles = allow renaming files: try to rename specified file first within common partition first; if failed - copy anyway
-deleteEmptyDirs = allow delete empty source directories after copy/move
-deleteCopiedFiles = allow delete successfully copied source files (ignored that specific file was renamed)
-ignoreExcludedPaths = ignore hardcoded restricted path parts like "Windows" or "Program Files" - normally, if source or destination file(s) contain those parts, it will be skipped