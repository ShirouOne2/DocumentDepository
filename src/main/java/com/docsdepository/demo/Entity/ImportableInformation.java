package com.docsdepository.demo.Entity; 

import jakarta.persistence.*;
import java.util.Date; 
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "importables_information") // Matches the table name
@DynamicUpdate
public class ImportableInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id") // CRITICAL: Must match the ID column name
    private Long id; 

    @Column(name = "document_classification_id") 
    private Integer classId; 

    @Column(name = "title") 
    private String title;

    @Column(name = "description") 
    private String description;

    @Column(name = "filename") 
    private String filename;

    // Assuming this column is the upload date/timestamp
    @Column(name = "date_created") 
    private String dateCreated; 

    // --- Constructors, Getters, and Setters (using camelCase) ---

    public ImportableInformation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    // ... all other getters and setters using classId, title, etc. ...
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }
    
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}