package com.edictum.web;

import com.edictum.model.Lesson;
import com.edictum.model.Section;
import com.edictum.service.LessonCatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class AdminLessonBean implements Serializable {
    @Inject
    private LessonCatalogService lessonCatalogService;

    private List<Lesson> lessons = new ArrayList<>();
    private List<Section> sections = new ArrayList<>();

    private Long editingLessonId;
    private Long sectionId;
    private String title;
    private String content;

    @PostConstruct
    public void init() {
        lessonCatalogService.ensureSeedData();
        loadData();
        resetForm();
    }

    public void saveLesson() {
        try {
            if (editingLessonId == null) {
                lessonCatalogService.createLesson(sectionId, title, content);
                addMessage(FacesMessage.SEVERITY_INFO, "Les toegevoegd.");
            } else {
                lessonCatalogService.updateLesson(editingLessonId, sectionId, title, content);
                addMessage(FacesMessage.SEVERITY_INFO, "Les bijgewerkt.");
            }
            loadData();
            resetForm();
        } catch (IllegalArgumentException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    public void editLesson(Lesson lesson) {
        editingLessonId = lesson.getId();
        sectionId = lesson.getSection().getId();
        title = lesson.getTitle();
        content = lesson.getContent();
    }

    public void deleteLesson(Long lessonId) {
        try {
            lessonCatalogService.deleteLesson(lessonId);
            addMessage(FacesMessage.SEVERITY_INFO, "Les verwijderd.");
            if (lessonId.equals(editingLessonId)) {
                resetForm();
            }
            loadData();
        } catch (IllegalArgumentException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
        }
    }

    public void cancelEdit() {
        resetForm();
    }

    private void loadData() {
        sections = lessonCatalogService.findAllSections();
        lessons = lessonCatalogService.findAllLessons();
    }

    private void resetForm() {
        editingLessonId = null;
        sectionId = sections.isEmpty() ? null : sections.get(0).getId();
        title = "";
        content = "";
    }

    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, null));
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public List<Section> getSections() {
        return sections;
    }

    public Long getEditingLessonId() {
        return editingLessonId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
