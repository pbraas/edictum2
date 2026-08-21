package com.edictum.web;

import com.edictum.service.LessonCatalogService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "lessonImageServlet", urlPatterns = "/lesson-images/*")
public class LessonImageServlet extends HttpServlet {
    @Inject
    private LessonCatalogService lessonCatalogService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long imageId = parseImageId(request.getPathInfo());
        if (imageId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        LessonCatalogService.LessonImageData imageData = lessonCatalogService.findLessonImage(imageId);
        if (imageData == null || imageData.getData() == null || imageData.getData().length == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(imageData.getContentType());
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setContentLength(imageData.getData().length);
        response.getOutputStream().write(imageData.getData());
    }

    private Long parseImageId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }

        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
