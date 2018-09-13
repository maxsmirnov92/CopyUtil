# CopyUtil
a small util to copy/move files or folders specified in list file from source directory to target directory

usage:
java -jar copyutil.jar -pathsListFile "" -sourcePath "" -destinationPath "" -renameFiles -deleteEmptyDirs -deleteCopiedFiles -ignoreExcludedPaths

root of partition is restricted for copy/move files (not folders) to

-pathsListFile = absolute path to file with relative paths that should be copied/moved; can be relative path to file or directory or may consists of only one separator char - that means thar all root content of source directory should be copied or source file should be copied itself (if it is file of course); separated with new line character; parameter is optional: if file empty it will be interpreted as one file separator in paths list; if contains spaces, must be wrapped in quotes
-excludeSourcePathsFile = absolute path to file with relative paths that should be excluded when copying/moving content from source directory; if contains spaces, must be wrapped in quotes
-sourcePath = source root absolute path: where copy/move files from; if contains spaces, must be wrapped in quotes
-destinationPath = destination root absolute path: where copy/move files to; if contains spaces, must be wrapped in quotes
-renameFiles = allow renaming files: try to rename specified file first within common partition first; if failed - copy anyway
-deleteEmptyDirs = allow delete empty source directories after copy/move
-deleteCopiedFiles = allow delete successfully copied source files (ignored that specific file was renamed)
-ignoreExcludedPaths = ignore hardcoded restricted path parts like "Windows" or "Program Files" - normally, if source or destination file(s) contain those parts, it will be skipped