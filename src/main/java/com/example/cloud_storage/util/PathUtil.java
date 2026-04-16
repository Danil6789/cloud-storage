package com.example.cloud_storage.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtil {
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        if (path.equals("/")) {
            return "";
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    public static String extractName(String path) {
        if (path.isEmpty()) return "";

        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        return lastSlash == -1 ? cleanPath : cleanPath.substring(lastSlash + 1);
    }

    public static String extractParentPath(String path) {
        if (path.isEmpty()) return "";

        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        return lastSlash == -1 ? "" : cleanPath.substring(0, lastSlash + 1);
    }

    public static String getRelativePath(String fullPath, String folderPath) {
        if (fullPath == null || folderPath == null) {
            return "";
        }

        if (fullPath.startsWith(folderPath)) {
            String relative = fullPath.substring(folderPath.length());
            return relative.isEmpty() ? "" : relative;
        }

        return fullPath;
    }
}