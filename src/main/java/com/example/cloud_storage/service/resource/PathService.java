package com.example.cloud_storage.service.resource;

import org.springframework.stereotype.Service;

@Service
public class PathService {
//    public String getCurrentUserRootPath() { //TODO: Задуматься насчёт того чтобы передавать id в сервс
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated()) {
//            throw new UnauthorizedException("User not authenticated");
//        }
//
//        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
//
//        return "user-" + userDetails.getId() + "-files/";
//    }

    public String getFullPath(Long userId, String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return String.format("user-%d-files/%s", userId, normalizedPath);
    }

    public String extractName(String path) {
        if (path.isEmpty()) return "";

        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        return lastSlash == -1 ? cleanPath : cleanPath.substring(lastSlash + 1);
    }

    public String extractParentPath(String path) {
        if (path.isEmpty()) return "";

        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        return lastSlash == -1 ? "" : cleanPath.substring(0, lastSlash + 1);
    }

    public String getRelativePath(String fullPath, String folderPath) {
        if (fullPath == null || folderPath == null) {
            return "";
        }

        if (fullPath.startsWith(folderPath)) {
            String relative = fullPath.substring(folderPath.length());
            return relative.isEmpty() ? "" : relative;
        }

        return fullPath;
    }

    //    public String normalizePath(String path) {
//        if (path == null || path.isBlank()) {
//            throw new IllegalArgumentException("Path cannot be empty");
//        }
//
//        if (path.equals("/")) {
//            return "";
//        }
//
//        return path.startsWith("/") ? path.substring(1) : path;
//    }
}