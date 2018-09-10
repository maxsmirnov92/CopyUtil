# CopyUtil
a small util to copy/move files or folders specified in list file from source directory to target directory

usage java -jar copyutil.jar "arg1" "arg2" "arg3" "arg4" "arg5" "arg6"
arg1 = absolute path to file with relative paths that should be copied/moved; can be relative path to file or directory; separated with new line character
arg2 = source root absolute path: where copy files from
arg3 = destination root absolute path: where copy files to
arg4 = allow renaming files: try to rename specified file first within common partition; if failed - copy anyway
arg5 = allow delete empty source directories after copy/move
arg6 = allow delete successfully copied source files