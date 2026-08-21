package com.edictum.web;

import com.edictum.model.Assignment;
import com.edictum.model.LearningMethod;
import com.edictum.model.Lesson;
import com.edictum.model.LessonGroup;
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
public class LearningPortalBean implements Serializable {
    @Inject
    private LessonCatalogService lessonCatalogService;
    @Inject
    private SecurityBean securityBean;

    private List<LessonGroup> groups = new ArrayList<>();
    private LearningMethod selectedMethod;
    private Section selectedSection;
    private Lesson selectedLesson;
    private boolean groupPanelVisible = true;
    private boolean lessonPanelVisible = true;
    private boolean editMode;
    private String editingTitle = "";
    private String editingContent = "";

    // add group
    private boolean addingGroup;
    private String newGroupName = "";

    // add method
    private boolean addingMethod;
    private Long addingMethodGroupId;
    private String newMethodName = "";

    // add lesson inline
    private boolean addingLesson;
    private String newLessonTitle = "";
    private String newLessonContent = "";

    // add / edit section
    private boolean addingSection;
    private String newSectionName = "";
    private boolean editingSection;
    private Long editingSectionId;
    private String editingSectionName = "";

    // assignments
    private Assignment viewingAssignment;
    private boolean assignmentDialogEditMode;
    private boolean addingAssignment;
    private String newAssignmentTitle = "";
    private String newAssignmentDescription = "";
    private Long editingAssignmentId;
    private String editingAssignmentTitle = "";
    private String editingAssignmentDescription = "";

    @PostConstruct
    public void init() {
        reload();
    }

    // â”€â”€ selection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void selectMethod(Long methodId) {
        cancelAllEditing();
        for (LessonGroup group : groups) {
            for (LearningMethod method : group.getMethods()) {
                if (method.getId().equals(methodId)) {
                    selectedMethod = method;
                    selectedSection = method.getSections().isEmpty() ? null : method.getSections().get(0);
                    selectedLesson = selectedSection == null || selectedSection.getLessons().isEmpty() ? null : selectedSection.getLessons().get(0);
                    return;
                }
            }
        }
    }

    public void selectSection(Long sectionId) {
        if (selectedMethod == null) {
            return;
        }
        cancelAllEditing();
        for (Section section : selectedMethod.getSections()) {
            if (section.getId().equals(sectionId)) {
                selectedSection = section;
                selectedLesson = section.getLessons().isEmpty() ? null : section.getLessons().get(0);
                return;
            }
        }
    }

    public void selectLesson(Long lessonId) {
        if (selectedSection == null) {
            return;
        }
        cancelAllEditing();
        for (Lesson lesson : selectedSection.getLessons()) {
            if (lesson.getId().equals(lessonId)) {
                selectedLesson = lesson;
                return;
            }
        }
    }

    public void selectNextLesson() {
        LessonSelection next = findNextLessonSelection();
        if (next == null) {
            return;
        }
        cancelAllEditing();
        selectedSection = next.section();
        selectedLesson = next.lesson();
    }

    public void selectPreviousLesson() {
        LessonSelection prev = findPreviousLessonSelection();
        if (prev == null) {
            return;
        }
        cancelAllEditing();
        selectedSection = prev.section();
        selectedLesson = prev.lesson();
    }

    // â”€â”€ panels â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void toggleLessonPanel() {
        lessonPanelVisible = !lessonPanelVisible;
    }

    public void toggleGroupPanel() {
        groupPanelVisible = !groupPanelVisible;
    }

    // â”€â”€ edit lesson â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void beginEditSelectedLesson() {
        if (!requireAdmin() || selectedLesson == null) {
            return;
        }
        cancelAddingLesson();
        editingTitle = selectedLesson.getTitle();
        editingContent = selectedLesson.getContent();
        editMode = true;
    }

    public void cancelEditSelectedLesson() {
        cancelEditState();
    }

    public void saveSelectedLesson() {
        if (!requireAdmin() || selectedLesson == null || selectedSection == null || selectedMethod == null) {
            return;
        }
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            lessonCatalogService.updateLesson(lessonId, sectionId, editingTitle, editingContent);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
            cancelEditState();
            addMessage(FacesMessage.SEVERITY_INFO, "Les bijgewerkt.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    // â”€â”€ add lesson inline â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void beginAddLesson() {
        if (!requireAdmin()) {
            return;
        }
        cancelEditState();
        newLessonTitle = "";
        newLessonContent = "";
        addingLesson = true;
    }

    public void cancelAddingLesson() {
        addingLesson = false;
        newLessonTitle = "";
        newLessonContent = "";
    }

    public void saveNewLesson() {
        if (!requireAdmin() || selectedSection == null || selectedMethod == null) {
            return;
        }
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = lessonCatalogService.createLesson(sectionId, newLessonTitle, newLessonContent);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
            cancelAddingLesson();
            addMessage(FacesMessage.SEVERITY_INFO, "Les toegevoegd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    // â”€â”€ delete lesson â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void deleteSelectedLesson() {
        if (!requireAdmin() || selectedLesson == null) {
            return;
        }
        deleteLesson(selectedLesson.getId());
    }

    public void deleteLesson(Long lessonId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            boolean wasSelected = selectedLesson != null && selectedLesson.getId().equals(lessonId);
            Long methodId = selectedMethod != null ? selectedMethod.getId() : null;
            Long sectionId = selectedSection != null ? selectedSection.getId() : null;
            lessonCatalogService.deleteLesson(lessonId);
            groups = lessonCatalogService.findAllGroups();
            if (wasSelected) {
                selectedLesson = null;
            }
            if (methodId != null) {
                restoreMethodAndSection(methodId, sectionId);
                if (wasSelected && selectedSection != null && !selectedSection.getLessons().isEmpty()) {
                    selectedLesson = selectedSection.getLessons().get(0);
                }
            }
            addMessage(FacesMessage.SEVERITY_INFO, "Les verwijderd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    // â”€â”€ move lesson â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void moveLessonUp(Long lessonId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            lessonCatalogService.moveLessonUp(lessonId);
            reloadKeepingSelection(lessonId);
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void moveLessonDown(Long lessonId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            lessonCatalogService.moveLessonDown(lessonId);
            reloadKeepingSelection(lessonId);
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    private void reloadKeepingSelection(Long lessonId) {
        Long methodId = selectedMethod != null ? selectedMethod.getId() : null;
        Long sectionId = selectedSection != null ? selectedSection.getId() : null;
        groups = lessonCatalogService.findAllGroups();
        if (methodId != null) {
            restoreSelection(methodId, sectionId, lessonId);
        }
    }

    // â”€â”€ add group â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void beginAddGroup() {
        if (!requireAdmin()) {
            return;
        }
        newGroupName = "";
        addingGroup = true;
    }

    public void cancelAddGroup() {
        addingGroup = false;
        newGroupName = "";
    }

    public void saveNewGroup() {
        if (!requireAdmin()) {
            return;
        }
        try {
            lessonCatalogService.createGroup(newGroupName);
            reload();
            cancelAddGroup();
            addMessage(FacesMessage.SEVERITY_INFO, "Groep toegevoegd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void deleteGroup(Long groupId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            boolean selectedAffected = selectedMethod != null && isMethodInGroup(groupId, selectedMethod.getId());
            lessonCatalogService.deleteGroup(groupId);
            reload();
            if (selectedAffected) {
                initializeSelection();
            }
            addMessage(FacesMessage.SEVERITY_INFO, "Groep verwijderd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    private boolean isMethodInGroup(Long groupId, Long methodId) {
        for (LessonGroup group : groups) {
            if (!group.getId().equals(groupId)) continue;
            for (LearningMethod method : group.getMethods()) {
                if (method.getId().equals(methodId)) return true;
            }
        }
        return false;
    }

    // â”€â”€ add method â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void beginAddMethod(Long groupId) {
        if (!requireAdmin()) {
            return;
        }
        newMethodName = "";
        addingMethodGroupId = groupId;
        addingMethod = true;
    }

    public void cancelAddMethod() {
        addingMethod = false;
        addingMethodGroupId = null;
        newMethodName = "";
    }

    public void saveNewMethod() {
        if (!requireAdmin()) {
            return;
        }
        try {
            lessonCatalogService.createMethod(addingMethodGroupId, newMethodName);
            Long methodId = selectedMethod != null ? selectedMethod.getId() : null;
            Long sectionId = selectedSection != null ? selectedSection.getId() : null;
            Long lessonId = selectedLesson != null ? selectedLesson.getId() : null;
            reload();
            if (methodId != null) restoreSelection(methodId, sectionId, lessonId);
            cancelAddMethod();
            addMessage(FacesMessage.SEVERITY_INFO, "Methode toegevoegd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void deleteMethod(Long methodId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            boolean selectedAffected = selectedMethod != null && selectedMethod.getId().equals(methodId);
            lessonCatalogService.deleteMethod(methodId);
            reload();
            if (selectedAffected) {
                initializeSelection();
            }
            addMessage(FacesMessage.SEVERITY_INFO, "Methode verwijderd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    // ── add / edit / delete section ───────────────────────────────────────────

    public void beginAddSection() {
        if (!requireAdmin() || selectedMethod == null) {
            return;
        }
        cancelEditSection();
        newSectionName = "";
        addingSection = true;
    }

    public void cancelAddSection() {
        addingSection = false;
        newSectionName = "";
    }

    public void saveNewSection() {
        if (!requireAdmin() || selectedMethod == null) {
            return;
        }
        try {
            Long methodId = selectedMethod.getId();
            Long lessonId = selectedLesson != null ? selectedLesson.getId() : null;
            Long newSectionId = lessonCatalogService.createSection(methodId, newSectionName);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, newSectionId, lessonId);
            cancelAddSection();
            addMessage(FacesMessage.SEVERITY_INFO, "Onderdeel toegevoegd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void beginEditSection(Long sectionId) {
        if (!requireAdmin() || selectedMethod == null) {
            return;
        }
        cancelAddSection();
        for (Section s : selectedMethod.getSections()) {
            if (s.getId().equals(sectionId)) {
                editingSectionId = sectionId;
                editingSectionName = s.getName();
                editingSection = true;
                return;
            }
        }
    }

    public void cancelEditSection() {
        editingSection = false;
        editingSectionId = null;
        editingSectionName = "";
    }

    public void saveEditedSection() {
        if (!requireAdmin() || editingSectionId == null) {
            return;
        }
        try {
            Long methodId = selectedMethod != null ? selectedMethod.getId() : null;
            Long sectionId = editingSectionId;
            Long lessonId = selectedLesson != null ? selectedLesson.getId() : null;
            lessonCatalogService.updateSection(sectionId, editingSectionName);
            groups = lessonCatalogService.findAllGroups();
            if (methodId != null) restoreSelection(methodId, sectionId, lessonId);
            cancelEditSection();
            addMessage(FacesMessage.SEVERITY_INFO, "Onderdeel bijgewerkt.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void deleteSection(Long sectionId) {
        if (!requireAdmin()) {
            return;
        }
        try {
            boolean wasSelected = selectedSection != null && selectedSection.getId().equals(sectionId);
            Long methodId = selectedMethod != null ? selectedMethod.getId() : null;
            Long keepSectionId = wasSelected ? null : (selectedSection != null ? selectedSection.getId() : null);
            lessonCatalogService.deleteSection(sectionId);
            groups = lessonCatalogService.findAllGroups();
            if (wasSelected) {
                selectedSection = null;
                selectedLesson = null;
            }
            if (methodId != null) restoreMethodAndSection(methodId, keepSectionId);
            addMessage(FacesMessage.SEVERITY_INFO, "Onderdeel verwijderd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    // ── assignments ───────────────────────────────────────────────────────────

    public void openAssignment(Long assignmentId) {
        if (selectedLesson == null) return;
        for (Assignment a : selectedLesson.getAssignments()) {
            if (a.getId().equals(assignmentId)) {
                viewingAssignment = a;
                assignmentDialogEditMode = false;
                return;
            }
        }
    }

    public void closeAssignmentDialog() {
        viewingAssignment = null;
        assignmentDialogEditMode = false;
        cancelEditAssignment();
    }

    public void beginAddAssignment() {
        if (!requireAdmin() || selectedLesson == null) return;
        newAssignmentTitle = "";
        newAssignmentDescription = "";
        addingAssignment = true;
    }

    public void cancelAddAssignment() {
        addingAssignment = false;
        newAssignmentTitle = "";
        newAssignmentDescription = "";
    }

    public void saveNewAssignment() {
        if (!requireAdmin() || selectedLesson == null || selectedSection == null || selectedMethod == null) return;
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            lessonCatalogService.createAssignment(lessonId, newAssignmentTitle, newAssignmentDescription);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
            cancelAddAssignment();
            addMessage(FacesMessage.SEVERITY_INFO, "Opdracht toegevoegd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void beginEditAssignmentInDialog() {
        if (!requireAdmin() || viewingAssignment == null) return;
        editingAssignmentId = viewingAssignment.getId();
        editingAssignmentTitle = viewingAssignment.getTitle();
        editingAssignmentDescription = viewingAssignment.getDescription();
        assignmentDialogEditMode = true;
    }

    public void cancelEditAssignment() {
        assignmentDialogEditMode = false;
        editingAssignmentId = null;
        editingAssignmentTitle = "";
        editingAssignmentDescription = "";
    }

    public void saveEditedAssignment() {
        if (!requireAdmin() || editingAssignmentId == null || selectedLesson == null || selectedSection == null || selectedMethod == null) return;
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            Long aId = editingAssignmentId;
            lessonCatalogService.updateAssignment(aId, editingAssignmentTitle, editingAssignmentDescription);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
            // refresh viewingAssignment from refreshed data
            if (selectedLesson != null) {
                for (Assignment a : selectedLesson.getAssignments()) {
                    if (a.getId().equals(aId)) { viewingAssignment = a; break; }
                }
            }
            cancelEditAssignment();
            addMessage(FacesMessage.SEVERITY_INFO, "Opdracht bijgewerkt.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void deleteAssignment(Long assignmentId) {
        if (!requireAdmin() || selectedLesson == null || selectedSection == null || selectedMethod == null) return;
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            if (viewingAssignment != null && viewingAssignment.getId().equals(assignmentId)) {
                viewingAssignment = null;
            }
            lessonCatalogService.deleteAssignment(assignmentId);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
            addMessage(FacesMessage.SEVERITY_INFO, "Opdracht verwijderd.");
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void moveAssignmentUp(Long assignmentId) {
        if (!requireAdmin() || selectedLesson == null || selectedSection == null || selectedMethod == null) return;
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            lessonCatalogService.moveAssignmentUp(assignmentId);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    public void moveAssignmentDown(Long assignmentId) {
        if (!requireAdmin() || selectedLesson == null || selectedSection == null || selectedMethod == null) return;
        try {
            Long methodId = selectedMethod.getId();
            Long sectionId = selectedSection.getId();
            Long lessonId = selectedLesson.getId();
            lessonCatalogService.moveAssignmentDown(assignmentId);
            groups = lessonCatalogService.findAllGroups();
            restoreSelection(methodId, sectionId, lessonId);
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    /** 1-based index of selectedLesson within selectedSection.lessons, or 0 if not found. */
    public int getSelectedLessonIndex() {
        if (selectedSection == null || selectedLesson == null) return 0;
        List<Lesson> lessons = selectedSection.getLessons();
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(selectedLesson.getId())) return i + 1;
        }
        return 0;
    }

    /** 1-based index of viewingAssignment within selectedLesson.assignments, or 0 if not found. */
    public int getViewingAssignmentIndex() {
        if (selectedLesson == null || viewingAssignment == null) return 0;
        List<Assignment> assignments = selectedLesson.getAssignments();
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getId().equals(viewingAssignment.getId())) return i + 1;
        }
        return 0;
    }

    // â”€â”€ reload / restore â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void reload() {
        groups = lessonCatalogService.findAllGroups();
        initializeSelection();
    }

    private void initializeSelection() {
        if (groups.isEmpty()) {
            selectedMethod = null;
            selectedSection = null;
            selectedLesson = null;
            return;
        }
        LessonGroup firstGroup = groups.get(0);
        if (firstGroup.getMethods().isEmpty()) {
            selectedMethod = null;
            selectedSection = null;
            selectedLesson = null;
            return;
        }
        selectedMethod = firstGroup.getMethods().get(0);
        selectedSection = selectedMethod.getSections().isEmpty() ? null : selectedMethod.getSections().get(0);
        selectedLesson = selectedSection == null || selectedSection.getLessons().isEmpty() ? null : selectedSection.getLessons().get(0);
    }

    private void restoreSelection(Long methodId, Long sectionId, Long lessonId) {
        for (LessonGroup group : groups) {
            for (LearningMethod method : group.getMethods()) {
                if (!method.getId().equals(methodId)) continue;
                selectedMethod = method;
                for (Section section : method.getSections()) {
                    if (!section.getId().equals(sectionId)) continue;
                    selectedSection = section;
                    if (lessonId != null) {
                        for (Lesson lesson : section.getLessons()) {
                            if (lesson.getId().equals(lessonId)) {
                                selectedLesson = lesson;
                                return;
                            }
                        }
                    }
                    selectedLesson = section.getLessons().isEmpty() ? null : section.getLessons().get(0);
                    return;
                }
                selectedSection = method.getSections().isEmpty() ? null : method.getSections().get(0);
                selectedLesson = selectedSection == null || selectedSection.getLessons().isEmpty() ? null : selectedSection.getLessons().get(0);
                return;
            }
        }
        initializeSelection();
    }

    private void restoreMethodAndSection(Long methodId, Long sectionId) {
        for (LessonGroup group : groups) {
            for (LearningMethod method : group.getMethods()) {
                if (!method.getId().equals(methodId)) continue;
                selectedMethod = method;
                if (sectionId != null) {
                    for (Section section : method.getSections()) {
                        if (section.getId().equals(sectionId)) {
                            selectedSection = section;
                            return;
                        }
                    }
                }
                selectedSection = method.getSections().isEmpty() ? null : method.getSections().get(0);
                return;
            }
        }
    }

    // â”€â”€ navigation helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private LessonSelection findNextLessonSelection() {
        if (selectedMethod == null || selectedSection == null || selectedLesson == null) return null;
        List<Section> sections = selectedMethod.getSections();
        for (int si = 0; si < sections.size(); si++) {
            List<Lesson> lessons = sections.get(si).getLessons();
            for (int li = 0; li < lessons.size(); li++) {
                if (!lessons.get(li).getId().equals(selectedLesson.getId())) continue;
                if (li + 1 < lessons.size()) return new LessonSelection(sections.get(si), lessons.get(li + 1));
                for (int nsi = si + 1; nsi < sections.size(); nsi++) {
                    if (!sections.get(nsi).getLessons().isEmpty())
                        return new LessonSelection(sections.get(nsi), sections.get(nsi).getLessons().get(0));
                }
                return null;
            }
        }
        return null;
    }

    private LessonSelection findPreviousLessonSelection() {
        if (selectedMethod == null || selectedSection == null || selectedLesson == null) return null;
        List<Section> sections = selectedMethod.getSections();
        for (int si = 0; si < sections.size(); si++) {
            List<Lesson> lessons = sections.get(si).getLessons();
            for (int li = 0; li < lessons.size(); li++) {
                if (!lessons.get(li).getId().equals(selectedLesson.getId())) continue;
                if (li - 1 >= 0) return new LessonSelection(sections.get(si), lessons.get(li - 1));
                for (int psi = si - 1; psi >= 0; psi--) {
                    List<Lesson> prev = sections.get(psi).getLessons();
                    if (!prev.isEmpty()) return new LessonSelection(sections.get(psi), prev.get(prev.size() - 1));
                }
                return null;
            }
        }
        return null;
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private boolean requireAdmin() {
        if (securityBean.isAdmin()) return true;
        addMessage(FacesMessage.SEVERITY_ERROR, "Beheerderstoegang vereist.");
        return false;
    }

    private void cancelEditState() {
        editMode = false;
        editingTitle = "";
        editingContent = "";
    }

    private void cancelAllEditing() {
        cancelEditState();
        cancelAddingLesson();
        cancelEditSection();
        cancelAddSection();
        cancelAddAssignment();
        cancelEditAssignment();
    }

    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, null));
    }

    // â”€â”€ getters / setters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public List<LessonGroup> getGroups() { return groups; }
    public LearningMethod getSelectedMethod() { return selectedMethod; }
    public Section getSelectedSection() { return selectedSection; }
    public Lesson getSelectedLesson() { return selectedLesson; }
    public boolean isNextLessonAvailable() { return findNextLessonSelection() != null; }
    public boolean isPreviousLessonAvailable() { return findPreviousLessonSelection() != null; }
    public boolean isLessonPanelVisible() { return lessonPanelVisible; }
    public boolean isGroupPanelVisible() { return groupPanelVisible; }
    public boolean isEditMode() { return editMode; }
    public boolean isAddingLesson() { return addingLesson; }
    public boolean isAddingGroup() { return addingGroup; }
    public boolean isAddingMethod() { return addingMethod; }
    public Long getAddingMethodGroupId() { return addingMethodGroupId; }
    public boolean isAddingSection() { return addingSection; }
    public boolean isEditingSection() { return editingSection; }
    public Long getEditingSectionId() { return editingSectionId; }

    public String getEditingTitle() { return editingTitle; }
    public void setEditingTitle(String editingTitle) { this.editingTitle = editingTitle; }
    public String getEditingContent() { return editingContent; }
    public void setEditingContent(String editingContent) { this.editingContent = editingContent; }
    public String getNewGroupName() { return newGroupName; }
    public void setNewGroupName(String newGroupName) { this.newGroupName = newGroupName; }
    public String getNewMethodName() { return newMethodName; }
    public void setNewMethodName(String newMethodName) { this.newMethodName = newMethodName; }
    public String getNewLessonTitle() { return newLessonTitle; }
    public void setNewLessonTitle(String newLessonTitle) { this.newLessonTitle = newLessonTitle; }
    public String getNewLessonContent() { return newLessonContent; }
    public void setNewLessonContent(String newLessonContent) { this.newLessonContent = newLessonContent; }
    public String getNewSectionName() { return newSectionName; }
    public void setNewSectionName(String newSectionName) { this.newSectionName = newSectionName; }
    public String getEditingSectionName() { return editingSectionName; }
    public void setEditingSectionName(String editingSectionName) { this.editingSectionName = editingSectionName; }

    public Assignment getViewingAssignment() { return viewingAssignment; }
    public boolean isAssignmentDialogEditMode() { return assignmentDialogEditMode; }
    public boolean isAddingAssignment() { return addingAssignment; }
    public String getNewAssignmentTitle() { return newAssignmentTitle; }
    public void setNewAssignmentTitle(String t) { this.newAssignmentTitle = t; }
    public String getNewAssignmentDescription() { return newAssignmentDescription; }
    public void setNewAssignmentDescription(String d) { this.newAssignmentDescription = d; }
    public String getEditingAssignmentTitle() { return editingAssignmentTitle; }
    public void setEditingAssignmentTitle(String t) { this.editingAssignmentTitle = t; }
    public String getEditingAssignmentDescription() { return editingAssignmentDescription; }
    public void setEditingAssignmentDescription(String d) { this.editingAssignmentDescription = d; }

    private record LessonSelection(Section section, Lesson lesson) {}
}
