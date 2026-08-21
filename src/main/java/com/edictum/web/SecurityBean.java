package com.edictum.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;

@Named
@RequestScoped
public class SecurityBean implements Serializable {
    @Inject
    private UserPreviewBean userPreviewBean;

    public boolean isLoggedIn() {
        return getExternalContext().getRemoteUser() != null;
    }

    /** True only when the user genuinely has the admin role AND is not in user-preview mode. */
    public boolean isAdmin() {
        return getExternalContext().isUserInRole("admin") && !userPreviewBean.isPreviewMode();
    }

    /** True when the user has the admin role, regardless of preview mode. */
    public boolean isRealAdmin() {
        return getExternalContext().isUserInRole("admin");
    }

    public boolean isTeacher() {
        return getExternalContext().isUserInRole("teacher");
    }

    public boolean isUser() {
        return getExternalContext().isUserInRole("user");
    }

    public String getRemoteUser() {
        String remoteUser = getExternalContext().getRemoteUser();
        return remoteUser == null ? "" : remoteUser;
    }

    public String getTinyMceScriptUrl() {
        String apiKey = System.getProperty("tinymce.api.key");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("TINYMCE_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = getExternalContext().getInitParameter("tinymce-api-key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "no-api-key";
        }
        return "https://cdn.tiny.cloud/1/" + apiKey + "/tinymce/7/tinymce.min.js";
    }

    public void logout() throws IOException, ServletException {
        ExternalContext externalContext = getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        request.logout();
        externalContext.invalidateSession();
        externalContext.redirect(externalContext.getRequestContextPath() + "/index.xhtml");
    }

    private ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}
