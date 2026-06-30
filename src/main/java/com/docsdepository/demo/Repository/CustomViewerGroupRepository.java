package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomViewerGroupRepository extends JpaRepository<CustomViewerGroup, Integer> {

    /**
     * Find active groups only
     */
    List<CustomViewerGroup> findByIsActiveTrueOrderByGroupNameAsc();

    /**
     * Find all groups ordered by name
     */
    List<CustomViewerGroup> findAllByOrderByGroupNameAsc();

    /**
     * Find group by name
     */
    Optional<CustomViewerGroup> findByGroupName(String groupName);

    /**
     * Check if group name exists
     */
    boolean existsByGroupName(String groupName);

    /**
     * Find groups created by a specific user
     */
    List<CustomViewerGroup> findByCreatedBy(Users createdBy);

    /**
     * Find groups where a specific user is a member
     */
    @Query("SELECT g FROM CustomViewerGroup g JOIN g.members m WHERE m.userId = :userId AND g.isActive = true")
    List<CustomViewerGroup> findGroupsByMemberId(@Param("userId") Integer userId);

    /**
     * Find active groups with member count
     */
    @Query("SELECT g FROM CustomViewerGroup g WHERE g.isActive = true ORDER BY g.groupName ASC")
    List<CustomViewerGroup> findAllActiveGroups();

    /**
     * Count members in a group
     */
    @Query("SELECT COUNT(m) FROM CustomViewerGroup g JOIN g.members m WHERE g.groupId = :groupId")
    Long countMembersByGroupId(@Param("groupId") Integer groupId);

    /**
     * Check if user is member of a specific group
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM CustomViewerGroup g JOIN g.members m " +
           "WHERE g.groupId = :groupId AND m.userId = :userId")
    boolean isUserMemberOfGroup(@Param("groupId") Integer groupId, @Param("userId") Integer userId);
}