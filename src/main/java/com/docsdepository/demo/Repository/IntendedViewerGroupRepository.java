package com.docsdepository.demo.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.docsdepository.demo.Entity.IntendedViewerGroup;

public interface IntendedViewerGroupRepository
        extends JpaRepository<IntendedViewerGroup, Integer> {
}

