package com.docsdepository.demo.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "office_viewer")
public class OfficeViewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "office_viewer_id")
    private Integer officeViewerId;

    @ManyToOne
    @JoinColumn(name = "intended_viewer_group_id", nullable = false)
    private IntendedViewerGroup intendedViewerGroup;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    // Constructors
    public OfficeViewer() {}

    public OfficeViewer(IntendedViewerGroup intendedViewerGroup, Office office) {
        this.intendedViewerGroup = intendedViewerGroup;
        this.office = office;
    }

    // Getters and Setters
    public Integer getOfficeViewerId() {
        return officeViewerId;
    }

    public void setOfficeViewerId(Integer officeViewerId) {
        this.officeViewerId = officeViewerId;
    }

    public IntendedViewerGroup getIntendedViewerGroup() {
        return intendedViewerGroup;
    }

    public void setIntendedViewerGroup(IntendedViewerGroup intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }
}