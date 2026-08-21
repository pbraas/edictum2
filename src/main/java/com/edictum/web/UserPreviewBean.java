package com.edictum.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@SessionScoped
public class UserPreviewBean implements Serializable {
    private boolean previewMode = false;

    public void toggle() {
        previewMode = !previewMode;
    }

    public boolean isPreviewMode() {
        return previewMode;
    }
}
