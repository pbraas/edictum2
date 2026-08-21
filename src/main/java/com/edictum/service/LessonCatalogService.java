package com.edictum.service;

import com.edictum.model.Assignment;
import com.edictum.model.LearningMethod;
import com.edictum.model.Lesson;
import com.edictum.model.LessonGroup;
import com.edictum.model.LessonImage;
import com.edictum.model.Section;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceUnit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@ApplicationScoped
public class LessonCatalogService {
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp"
    );

    @PersistenceUnit(unitName = "edictum2PU")
    private EntityManagerFactory entityManagerFactory;

    public void ensureSeedData() {
        executeInTransaction(entityManager -> {
            persistGroupIfMissing(entityManager, buildProgrammingGroup());
            persistGroupIfMissing(entityManager, buildSkillsGroup());
            persistGroupIfMissing(entityManager, buildDataScienceGroup());
            persistGroupIfMissing(entityManager, buildDesignGroup());
            persistGroupIfMissing(entityManager, buildBusinessGroup());
            persistGroupIfMissing(entityManager, buildLanguageLearningGroup());
        });
    }

    private void persistGroupIfMissing(EntityManager entityManager, LessonGroup group) {
        Long existingCount = entityManager.createQuery(
                "SELECT COUNT(g) FROM LessonGroup g WHERE g.name = :name",
                Long.class
        ).setParameter("name", group.getName()).getSingleResult();
        if (existingCount == 0) {
            entityManager.persist(group);
        }
    }

    private LessonGroup buildProgrammingGroup() {
        LessonGroup programming = new LessonGroup();
        programming.setName("Programming");

        LearningMethod javaBasics = new LearningMethod();
        javaBasics.setName("Java Basics");
        programming.addMethod(javaBasics);

        Section syntaxSection = new Section();
        syntaxSection.setName("Syntax");
        javaBasics.addSection(syntaxSection);
        syntaxSection.addLesson(new Lesson(null, "Variables and Types", "Understand primitive and reference types, declaration rules, and naming conventions."));
        syntaxSection.addLesson(new Lesson(null, "Control Flow", "Use if/else, switch, loops, and flow control statements effectively."));

        Section oopSection = new Section();
        oopSection.setName("Object-Oriented Programming");
        javaBasics.addSection(oopSection);
        oopSection.addLesson(new Lesson(null, "Classes and Objects", "Create classes, instantiate objects, and model problem domains."));
        oopSection.addLesson(new Lesson(null, "Inheritance and Interfaces", "Apply reuse with inheritance and abstraction with interfaces."));

        LearningMethod webDevelopment = new LearningMethod();
        webDevelopment.setName("Web Development");
        programming.addMethod(webDevelopment);

        Section jsfSection = new Section();
        jsfSection.setName("JSF and PrimeFaces");
        webDevelopment.addSection(jsfSection);
        jsfSection.addLesson(new Lesson(null, "Managed Beans", "Bind UI components to managed beans and keep view state predictable."));
        jsfSection.addLesson(new Lesson(null, "PrimeFaces Components", "Build responsive pages with data tables, dialogs, and interactive controls."));
        return programming;
    }

    private LessonGroup buildSkillsGroup() {
        LessonGroup skills = new LessonGroup();
        skills.setName("Skills");

        LearningMethod teamwork = new LearningMethod();
        teamwork.setName("Teamwork");
        skills.addMethod(teamwork);

        Section communicationSection = new Section();
        communicationSection.setName("Communication");
        teamwork.addSection(communicationSection);
        communicationSection.addLesson(new Lesson(null, "Active Listening", "Use clarifying questions and summaries to reduce misunderstandings."));
        communicationSection.addLesson(new Lesson(null, "Constructive Feedback", "Deliver feedback with clear context, examples, and actionable suggestions."));

        LearningMethod productivity = new LearningMethod();
        productivity.setName("Productivity");
        skills.addMethod(productivity);

        Section planningSection = new Section();
        planningSection.setName("Planning");
        productivity.addSection(planningSection);
        planningSection.addLesson(new Lesson(null, "Prioritization", "Use impact and effort to organize work and avoid context switching."));
        planningSection.addLesson(new Lesson(null, "Time Blocking", "Reserve focused time slots for deep work and learning."));
        return skills;
    }

    private LessonGroup buildDataScienceGroup() {
        LessonGroup dataScience = new LessonGroup();
        dataScience.setName("Data Science");

        LearningMethod analytics = new LearningMethod();
        analytics.setName("Data Analytics");
        dataScience.addMethod(analytics);

        Section analysisSection = new Section();
        analysisSection.setName("Data Analysis");
        analytics.addSection(analysisSection);
        analysisSection.addLesson(new Lesson(null, "Exploratory Data Analysis", "Use summary statistics and visual exploration to understand distributions and anomalies."));
        analysisSection.addLesson(new Lesson(null, "Feature Engineering Basics", "Create meaningful features by transforming, combining, and encoding raw data."));

        LearningMethod machineLearning = new LearningMethod();
        machineLearning.setName("Machine Learning");
        dataScience.addMethod(machineLearning);

        Section mlSection = new Section();
        mlSection.setName("Supervised Learning");
        machineLearning.addSection(mlSection);
        mlSection.addLesson(new Lesson(null, "Regression Fundamentals", "Train and evaluate regression models using validation data and error metrics."));
        mlSection.addLesson(new Lesson(null, "Classification Workflows", "Build classification pipelines and compare models using precision, recall, and F1."));
        return dataScience;
    }

    private LessonGroup buildDesignGroup() {
        LessonGroup design = new LessonGroup();
        design.setName("Design");

        LearningMethod ux = new LearningMethod();
        ux.setName("UX Design");
        design.addMethod(ux);

        Section researchSection = new Section();
        researchSection.setName("User Research");
        ux.addSection(researchSection);
        researchSection.addLesson(new Lesson(null, "Interview Planning", "Design interview guides to uncover user needs, goals, and friction points."));
        researchSection.addLesson(new Lesson(null, "Journey Mapping", "Visualize user journeys to identify moments of confusion and opportunity."));

        LearningMethod ui = new LearningMethod();
        ui.setName("UI Design");
        design.addMethod(ui);

        Section interfaceSection = new Section();
        interfaceSection.setName("Interface Design");
        ui.addSection(interfaceSection);
        interfaceSection.addLesson(new Lesson(null, "Visual Hierarchy", "Use spacing, contrast, and typography to guide attention and improve readability."));
        interfaceSection.addLesson(new Lesson(null, "Component Consistency", "Build repeatable UI patterns with predictable behavior and clear states."));
        return design;
    }

    private LessonGroup buildBusinessGroup() {
        LessonGroup business = new LessonGroup();
        business.setName("Business");

        LearningMethod strategy = new LearningMethod();
        strategy.setName("Strategy");
        business.addMethod(strategy);

        Section planningSection = new Section();
        planningSection.setName("Strategic Planning");
        strategy.addSection(planningSection);
        planningSection.addLesson(new Lesson(null, "Market Positioning", "Define target segments and craft value propositions that differentiate your offer."));
        planningSection.addLesson(new Lesson(null, "Goal Setting", "Translate strategy into measurable quarterly goals and practical initiatives."));

        LearningMethod operations = new LearningMethod();
        operations.setName("Operations");
        business.addMethod(operations);

        Section processSection = new Section();
        processSection.setName("Process Management");
        operations.addSection(processSection);
        processSection.addLesson(new Lesson(null, "Workflow Mapping", "Document key processes and remove bottlenecks with clear ownership."));
        processSection.addLesson(new Lesson(null, "Performance Metrics", "Track operational health with leading and lagging indicators."));
        return business;
    }

    private LessonGroup buildLanguageLearningGroup() {
        LessonGroup languageLearning = new LessonGroup();
        languageLearning.setName("Language Learning");

        LearningMethod speaking = new LearningMethod();
        speaking.setName("Speaking");
        languageLearning.addMethod(speaking);

        Section pronunciationSection = new Section();
        pronunciationSection.setName("Pronunciation");
        speaking.addSection(pronunciationSection);
        pronunciationSection.addLesson(new Lesson(null, "Sound Practice", "Practice challenging sounds and stress patterns through short speaking drills."));
        pronunciationSection.addLesson(new Lesson(null, "Conversation Starters", "Use structured prompts to build confidence in everyday dialogues."));

        LearningMethod writing = new LearningMethod();
        writing.setName("Writing");
        languageLearning.addMethod(writing);

        Section grammarSection = new Section();
        grammarSection.setName("Grammar and Writing");
        writing.addSection(grammarSection);
        grammarSection.addLesson(new Lesson(null, "Sentence Structure", "Construct clear sentences using correct word order and agreement."));
        grammarSection.addLesson(new Lesson(null, "Paragraph Development", "Organize ideas into coherent paragraphs with transitions and examples."));
        return languageLearning;
    }

    public List<LessonGroup> findAllGroups() {
        entityManagerFactory.getCache().evictAll();
        return executeQuery(entityManager -> {
            List<LessonGroup> groups = entityManager.createQuery(
                    "SELECT DISTINCT g FROM LessonGroup g " +
                            "LEFT JOIN FETCH g.methods m " +
                            "LEFT JOIN FETCH m.sections s " +
                            "LEFT JOIN FETCH s.lessons l " +
                            "LEFT JOIN FETCH l.assignments a " +
                            "ORDER BY g.id, m.id, s.sortOrder, s.id, l.sortOrder, l.id, a.sortOrder, a.id",
                    LessonGroup.class
            )
                    .setHint("eclipselink.refresh", "true")
                    .setHint("eclipselink.maintain-cache", "false")
                    .getResultList();

            for (LessonGroup group : groups) {
                group.getMethods().sort((left, right) -> left.getId().compareTo(right.getId()));
                for (LearningMethod method : group.getMethods()) {
                    method.getSections().sort((left, right) -> {
                        int s = Integer.compare(left.getSortOrder(), right.getSortOrder());
                        return s != 0 ? s : left.getId().compareTo(right.getId());
                    });
                    for (Section section : method.getSections()) {
                        section.getLessons().sort((left, right) -> {
                            int sortOrderCompare = Integer.compare(left.getSortOrder(), right.getSortOrder());
                            return sortOrderCompare != 0 ? sortOrderCompare : left.getId().compareTo(right.getId());
                        });
                        for (Lesson lesson : section.getLessons()) {
                            lesson.getAssignments().sort((left, right) -> {
                                int s = Integer.compare(left.getSortOrder(), right.getSortOrder());
                                return s != 0 ? s : left.getId().compareTo(right.getId());
                            });
                        }
                    }
                }
            }
            return groups;
        });
    }

    public List<Section> findAllSections() {
        return executeQuery(entityManager -> entityManager.createQuery(
                "SELECT s FROM Section s " +
                        "JOIN FETCH s.method m " +
                        "JOIN FETCH m.group " +
                        "ORDER BY m.group.id, m.id, s.id",
                Section.class
        ).getResultList());
    }

    public List<Lesson> findAllLessons() {
        return executeQuery(entityManager -> entityManager.createQuery(
                "SELECT l FROM Lesson l " +
                        "JOIN FETCH l.section s " +
                        "JOIN FETCH s.method m " +
                        "JOIN FETCH m.group " +
                        "ORDER BY m.group.id, m.id, s.id, l.id",
                Lesson.class
        ).getResultList());
    }

    public Long createSection(Long methodId, String name) {
        return executeInTransactionWithResult(entityManager -> {
            if (methodId == null) {
                throw new IllegalArgumentException("Methode is verplicht.");
            }
            LearningMethod method = entityManager.find(LearningMethod.class, methodId);
            if (method == null) {
                throw new IllegalArgumentException("Methode niet gevonden.");
            }
            String normalized = normalizeRequired(name, "Naam onderdeel");
            Integer maxSortOrder = entityManager.createQuery(
                    "SELECT MAX(s.sortOrder) FROM Section s WHERE s.method = :method",
                    Integer.class
            ).setParameter("method", method).getSingleResult();
            Section section = new Section();
            section.setName(normalized);
            section.setMethod(method);
            section.setSortOrder((maxSortOrder == null ? -10 : maxSortOrder) + 10);
            entityManager.persist(section);
            entityManager.flush();
            return section.getId();
        });
    }

    public void updateSection(Long sectionId, String name) {
        executeInTransaction(entityManager -> {
            if (sectionId == null) {
                throw new IllegalArgumentException("Onderdeel niet gevonden.");
            }
            Section section = entityManager.find(Section.class, sectionId);
            if (section == null) {
                throw new IllegalArgumentException("Onderdeel niet gevonden.");
            }
            section.setName(normalizeRequired(name, "Naam onderdeel"));
        });
    }

    public void deleteSection(Long sectionId) {
        executeInTransaction(entityManager -> {
            if (sectionId == null) {
                throw new IllegalArgumentException("Onderdeel niet gevonden.");
            }
            Section section = entityManager.find(Section.class, sectionId);
            if (section == null) {
                throw new IllegalArgumentException("Onderdeel niet gevonden.");
            }
            Long lessonCount = entityManager.createQuery(
                    "SELECT COUNT(l) FROM Lesson l WHERE l.section = :section",
                    Long.class
            ).setParameter("section", section).getSingleResult();
            if (lessonCount > 0) {
                throw new IllegalArgumentException(
                        "Onderdeel '" + section.getName() + "' bevat nog " + lessonCount +
                        " les(sen). Verwijder eerst alle lessen voordat u het onderdeel verwijdert.");
            }
            entityManager.remove(section);
        });
    }

    public Long createLesson(Long sectionId, String title, String content) {
        return executeInTransactionWithResult(entityManager -> {
            if (sectionId == null) {
                throw new IllegalArgumentException("Section is required.");
            }
            Section section = entityManager.find(Section.class, sectionId);
            if (section == null) {
                throw new IllegalArgumentException("Section not found.");
            }

            Lesson lesson = new Lesson();
            lesson.setTitle(normalizeRequired(title, "Lestitel"));
            lesson.setContent(Objects.requireNonNullElse(content, "").trim());
            lesson.setSection(section);
            Integer maxSortOrder = entityManager.createQuery(
                    "SELECT MAX(l.sortOrder) FROM Lesson l WHERE l.section = :section",
                    Integer.class
            ).setParameter("section", section).getSingleResult();
            lesson.setSortOrder((maxSortOrder == null ? -10 : maxSortOrder) + 10);
            entityManager.persist(lesson);
            entityManager.flush();
            return lesson.getId();
        });
    }

    public void updateLesson(Long lessonId, Long sectionId, String title, String content) {
        executeInTransaction(entityManager -> {
            if (lessonId == null) {
                throw new IllegalArgumentException("Lesson not found.");
            }
            if (sectionId == null) {
                throw new IllegalArgumentException("Section is required.");
            }
            Lesson lesson = entityManager.find(Lesson.class, lessonId);
            if (lesson == null) {
                throw new IllegalArgumentException("Lesson not found.");
            }

            Section section = entityManager.find(Section.class, sectionId);
            if (section == null) {
                throw new IllegalArgumentException("Section not found.");
            }

            lesson.setTitle(normalizeRequired(title, "Title"));
            lesson.setContent(normalizeRequired(content, "Content"));
            lesson.setSection(section);
        });
    }

    public void deleteLesson(Long lessonId) {
        executeInTransaction(entityManager -> {
            if (lessonId == null) {
                throw new IllegalArgumentException("Lesson not found.");
            }
            Lesson lesson = entityManager.find(Lesson.class, lessonId);
            if (lesson == null) {
                throw new IllegalArgumentException("Lesson not found.");
            }
            entityManager.remove(lesson);
        });
    }

    public Long createAssignment(Long lessonId, String title, String description) {
        return executeInTransactionWithResult(entityManager -> {
            if (lessonId == null) {
                throw new IllegalArgumentException("Les is verplicht.");
            }
            Lesson lesson = entityManager.find(Lesson.class, lessonId);
            if (lesson == null) {
                throw new IllegalArgumentException("Les niet gevonden.");
            }
            String normalizedTitle = normalizeRequired(title, "Opdrachttitel");
            Integer maxSortOrder = entityManager.createQuery(
                    "SELECT MAX(a.sortOrder) FROM Assignment a WHERE a.lesson = :lesson",
                    Integer.class
            ).setParameter("lesson", lesson).getSingleResult();
            Assignment assignment = new Assignment();
            assignment.setTitle(normalizedTitle);
            assignment.setDescription(Objects.requireNonNullElse(description, "").trim());
            assignment.setLesson(lesson);
            assignment.setSortOrder((maxSortOrder == null ? -10 : maxSortOrder) + 10);
            entityManager.persist(assignment);
            entityManager.flush();
            return assignment.getId();
        });
    }

    public void updateAssignment(Long assignmentId, String title, String description) {
        executeInTransaction(entityManager -> {
            if (assignmentId == null) {
                throw new IllegalArgumentException("Opdracht niet gevonden.");
            }
            Assignment assignment = entityManager.find(Assignment.class, assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Opdracht niet gevonden.");
            }
            assignment.setTitle(normalizeRequired(title, "Opdrachttitel"));
            assignment.setDescription(Objects.requireNonNullElse(description, "").trim());
        });
    }

    public void deleteAssignment(Long assignmentId) {
        executeInTransaction(entityManager -> {
            if (assignmentId == null) {
                throw new IllegalArgumentException("Opdracht niet gevonden.");
            }
            Assignment assignment = entityManager.find(Assignment.class, assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Opdracht niet gevonden.");
            }
            entityManager.remove(assignment);
        });
    }

    public void moveAssignmentUp(Long assignmentId) {
        executeInTransaction(entityManager -> {
            Assignment assignment = entityManager.find(Assignment.class, assignmentId);
            if (assignment != null) reorderAssignmentWithinLesson(entityManager, assignment, -1);
        });
    }

    public void moveAssignmentDown(Long assignmentId) {
        executeInTransaction(entityManager -> {
            Assignment assignment = entityManager.find(Assignment.class, assignmentId);
            if (assignment != null) reorderAssignmentWithinLesson(entityManager, assignment, 1);
        });
    }

    private void reorderAssignmentWithinLesson(EntityManager entityManager, Assignment assignment, int direction) {
        List<Assignment> assignments = entityManager.createQuery(
                "SELECT a FROM Assignment a WHERE a.lesson.id = :lessonId ORDER BY a.sortOrder ASC, a.id ASC",
                Assignment.class
        ).setParameter("lessonId", assignment.getLesson().getId()).getResultList();

        int idx = -1;
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getId().equals(assignment.getId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return;
        int swapIdx = idx + direction;
        if (swapIdx < 0 || swapIdx >= assignments.size()) return;
        Collections.swap(assignments, idx, swapIdx);
        for (int i = 0; i < assignments.size(); i++) {
            assignments.get(i).setSortOrder(i * 10);
        }
        entityManager.flush();
    }


    public Long createGroup(String name) {
        return executeInTransactionWithResult(entityManager -> {
            String normalized = normalizeRequired(name, "Naam");
            LessonGroup group = new LessonGroup();
            group.setName(normalized);
            entityManager.persist(group);
            entityManager.flush();
            return group.getId();
        });
    }

    public void deleteGroup(Long groupId) {
        executeInTransaction(entityManager -> {
            if (groupId == null) {
                throw new IllegalArgumentException("Groep niet gevonden.");
            }
            LessonGroup group = entityManager.find(LessonGroup.class, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Groep niet gevonden.");
            }
            Long methodCount = entityManager.createQuery(
                    "SELECT COUNT(m) FROM LearningMethod m WHERE m.group = :group",
                    Long.class
            ).setParameter("group", group).getSingleResult();
            if (methodCount > 0) {
                throw new IllegalArgumentException(
                        "Groep '" + group.getName() + "' bevat nog " + methodCount +
                        " methode(n). Verwijder eerst alle methoden voordat u de groep verwijdert.");
            }
            entityManager.remove(group);
        });
    }

    public Long createMethod(Long groupId, String name) {
        return executeInTransactionWithResult(entityManager -> {
            if (groupId == null) {
                throw new IllegalArgumentException("Groep is verplicht.");
            }
            LessonGroup group = entityManager.find(LessonGroup.class, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Groep niet gevonden.");
            }
            String normalized = normalizeRequired(name, "Naam");
            LearningMethod method = new LearningMethod();
            method.setName(normalized);
            method.setGroup(group);
            entityManager.persist(method);
            entityManager.flush();
            return method.getId();
        });
    }

    public void deleteMethod(Long methodId) {
        executeInTransaction(entityManager -> {
            if (methodId == null) {
                throw new IllegalArgumentException("Methode niet gevonden.");
            }
            LearningMethod method = entityManager.find(LearningMethod.class, methodId);
            if (method == null) {
                throw new IllegalArgumentException("Methode niet gevonden.");
            }
            Long lessonCount = entityManager.createQuery(
                    "SELECT COUNT(l) FROM Lesson l WHERE l.section.method = :method",
                    Long.class
            ).setParameter("method", method).getSingleResult();
            if (lessonCount > 0) {
                throw new IllegalArgumentException(
                        "Methode '" + method.getName() + "' bevat nog " + lessonCount +
                        " les(sen). Verwijder eerst alle lessen voordat u de methode verwijdert.");
            }
            entityManager.remove(method);
        });
    }

    public void moveLessonUp(Long lessonId) {
        executeInTransaction(entityManager -> {
            Lesson lesson = entityManager.find(Lesson.class, lessonId);
            if (lesson == null) {
                return;
            }
            reorderLessonWithinSection(entityManager, lesson, -1);
        });
    }

    public void moveLessonDown(Long lessonId) {
        executeInTransaction(entityManager -> {
            Lesson lesson = entityManager.find(Lesson.class, lessonId);
            if (lesson == null) {
                return;
            }
            reorderLessonWithinSection(entityManager, lesson, 1);
        });
    }

    private void reorderLessonWithinSection(EntityManager entityManager, Lesson lesson, int direction) {
        List<Lesson> lessons = entityManager.createQuery(
                "SELECT l FROM Lesson l WHERE l.section.id = :sectionId ORDER BY l.sortOrder ASC, l.id ASC",
                Lesson.class
        ).setParameter("sectionId", lesson.getSection().getId()).getResultList();

        int idx = -1;
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lesson.getId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }

        int swapIdx = idx + direction;
        if (swapIdx < 0 || swapIdx >= lessons.size()) {
            return;
        }

        Collections.swap(lessons, idx, swapIdx);
        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).setSortOrder(i * 10);
        }
        entityManager.flush();
    }

    public Long storeLessonImage(String originalFilename, String contentType, byte[] data) {
        return executeInTransactionWithResult(entityManager -> {
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Image file is required.");
            }
            if (data.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("Image exceeds 5 MB limit.");
            }

            String normalizedType = Objects.requireNonNullElse(contentType, "").trim().toLowerCase();
            if (!ALLOWED_IMAGE_TYPES.contains(normalizedType)) {
                throw new IllegalArgumentException("Unsupported image type. Use PNG, JPEG, GIF, or WEBP.");
            }

            String normalizedFilename = Objects.requireNonNullElse(originalFilename, "").trim();
            if (normalizedFilename.isEmpty()) {
                normalizedFilename = "image";
            }
            if (normalizedFilename.length() > 255) {
                normalizedFilename = normalizedFilename.substring(0, 255);
            }

            LessonImage image = new LessonImage();
            image.setOriginalFilename(normalizedFilename);
            image.setContentType(normalizedType);
            image.setData(data);
            entityManager.persist(image);
            entityManager.flush();
            return image.getId();
        });
    }

    public LessonImageData findLessonImage(Long imageId) {
        if (imageId == null) {
            return null;
        }
        return executeQuery(entityManager -> {
            LessonImage image = entityManager.find(LessonImage.class, imageId);
            if (image == null) {
                return null;
            }
            return new LessonImageData(image.getContentType(), image.getData());
        });
    }

    public static final class LessonImageData {
        private final String contentType;
        private final byte[] data;

        public LessonImageData(String contentType, byte[] data) {
            this.contentType = contentType;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private <T> T executeQuery(Function<EntityManager, T> callback) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return callback.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    private void executeInTransaction(Consumer<EntityManager> callback) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            callback.accept(entityManager);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        } finally {
            entityManager.close();
        }
    }

    private <T> T executeInTransactionWithResult(Function<EntityManager, T> callback) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = callback.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        } finally {
            entityManager.close();
        }
    }
}
