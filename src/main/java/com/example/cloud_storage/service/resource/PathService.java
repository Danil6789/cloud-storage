package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.user.UserDetailsImpl;
import com.example.cloud_storage.exception.auth.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static com.example.cloud_storage.constant.ExceptionMessages.UNAUTHORIZED;

@Service
public class PathService {

    private static final String USER_DIR_TEMPLATE = "user-%d-files/";
    private static final String USER_DIR_PATTERN = "^user-\\d+-files/";


    public String getFullPath(String path) {;
        return getCurrentUserRootPath() + path;
    }

    public String extractName(String path) {
        if (path.isEmpty()) return "";

        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        return lastSlash == -1 ? path : path.substring(lastSlash + 1);
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

    public String removeUserDirPrefix(String path) {
        return path.replaceFirst(USER_DIR_PATTERN, "");
    }

    public String getCurrentUserRootPath() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException(UNAUTHORIZED);
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        return USER_DIR_TEMPLATE.formatted(userDetails.getId());
    }
}