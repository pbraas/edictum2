package com.edictum.web;

import com.edictum.service.LessonCatalogService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;

@WebServlet(name = "lessonImageUploadServlet", urlPatterns = "/lesson-images/upload")
@MultipartConfig
public class LessonImageUploadServlet extends HttpServlet {
    @Inject
    private LessonCatalogService lessonCatalogService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("TinyMCE upload debug: principal=" + request.getUserPrincipal());
        System.out.println("TinyMCE upload debug: admin=" + request.isUserInRole("admin"));
        System.out.println("TinyMCE upload debug: teacher=" + request.isUserInRole("teacher"));
        System.out.println("TinyMCE upload debug: user=" + request.isUserInRole("user"));

        if (request.getUserPrincipal() == null) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Not authenticated.");
            return;
        }

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Image file is required.");
            return;
        }

        try {
            byte[] fileData = filePart.getInputStream().readAllBytes();
            Long imageId = lessonCatalogService.storeLessonImage(
                    filePart.getSubmittedFileName(),
                    filePart.getContentType(),
                    fileData
            );

            String imageUrl = request.getContextPath() + "/lesson-images/" + imageId;
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"location\":\"" + escapeJson(imageUrl) + "\"}");
        } catch (IllegalArgumentException ex) {
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (RuntimeException ex) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image upload failed.");
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}