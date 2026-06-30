package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.CustomViewerGroupRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomViewerGroupService {

    @Autowired
    private CustomViewerGroupRepository groupRepository;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Get all active groups
     */
    public List<CustomViewerGroup> getAllActiveGroups() {
        return groupRepository.findByIsActiveTrueOrderByGroupNameAsc();
    }

    /**
     * Get all groups (including inactive)
     */
    public List<CustomViewerGroup> getAllGroups() {
        return groupRepository.findAllByOrderByGroupNameAsc();
    }

    /**
     * Get group by ID
     */
    public Optional<CustomViewerGroup> getGroupById(Integer groupId) {
        return groupRepository.findById(groupId);
    }

    /**
     * Create a new custom viewer group
     */
    @Transactional
    public CustomViewerGroup createGroup(String groupName, String description, Users createdBy, Set<Integer> memberIds) {
        // Check if group name already exists
        if (groupRepository.existsByGroupName(groupName)) {
            throw new RuntimeException("Group name already exists");
        }

        // Create new group
        CustomViewerGroup group = new CustomViewerGroup(groupName, description, createdBy);

        // Add members if provided
        if (memberIds != null && !memberIds.isEmpty()) {
            Set<Users> members = memberIds.stream()
                    .map(userId -> usersRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId)))
                    .collect(Collectors.toSet());
            group.setMembers(members);
        }

        return groupRepository.save(group);
    }

    /**
     * Update an existing group
     */
    @Transactional
    public CustomViewerGroup updateGroup(Integer groupId, String groupName, String description, Set<Integer> memberIds) {
        CustomViewerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if new name conflicts with another group
        if (!group.getGroupName().equals(groupName) && groupRepository.existsByGroupName(groupName)) {
            throw new RuntimeException("Group name already exists");
        }

        group.setGroupName(groupName);
        group.setDescription(description);

        // Update members
        if (memberIds != null) {
            Set<Users> members = memberIds.stream()
                    .map(userId -> usersRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId)))
                    .collect(Collectors.toSet());
            group.setMembers(members);
        }

        return groupRepository.save(group);
    }

    /**
     * Add member to group
     */
    @Transactional
    public void addMember(Integer groupId, Integer userId) {
        CustomViewerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.addMember(user);
        groupRepository.save(group);
    }

    /**
     * Remove member from group
     */
    @Transactional
    public void removeMember(Integer groupId, Integer userId) {
        CustomViewerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.removeMember(user);
        groupRepository.save(group);
    }

    /**
     * Delete a group (soft delete by setting isActive = false)
     */
    @Transactional
    public void deleteGroup(Integer groupId) {
        CustomViewerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        group.setIsActive(false);
        groupRepository.save(group);
    }

    /**
     * Permanently delete a group (hard delete)
     */
    @Transactional
    public void permanentlyDeleteGroup(Integer groupId) {
        groupRepository.deleteById(groupId);
    }

    /**
     * Check if user is member of a group
     */
    public boolean isUserMemberOfGroup(Integer groupId, Integer userId) {
        return groupRepository.isUserMemberOfGroup(groupId, userId);
    }

    /**
     * Get all groups a user belongs to
     */
    public List<CustomViewerGroup> getGroupsByUserId(Integer userId) {
        return groupRepository.findGroupsByMemberId(userId);
    }

    /**
     * Get member count for a group
     */
    public Long getMemberCount(Integer groupId) {
        return groupRepository.countMembersByGroupId(groupId);
    }

    /**
     * Check if user can view a document with custom viewer group
     */
    public boolean canUserViewDocument(Integer customGroupId, Integer userId) {
        if (customGroupId == null || userId == null) {
            return false;
        }
        return isUserMemberOfGroup(customGroupId, userId);
    }
}