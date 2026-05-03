package com.groute.groute_server.record.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.ProjectUseCase;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/** 프로젝트 태그 CRUD 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService implements ProjectUseCase {

    private final ProjectPort projectPort;
    private final UserPort userPort;

    @Transactional
    @Override
    public Project createProject(Long userId, String name) {
        if (projectPort.existsByUserIdAndName(userId, name)) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE);
        }
        User user = userPort.findById(userId);
        Project project = Project.builder().user(user).name(name).build();
        return projectPort.save(project);
    }

    @Override
    public Page<Project> getProjects(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return projectPort.findAllByUserId(userId, pageable);
    }

    @Transactional
    @Override
    public Project updateProject(Long userId, Long projectId, String name) {
        Project project =
                projectPort
                        .findByIdAndUserId(projectId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        if (projectPort.existsByUserIdAndName(userId, name)) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE);
        }
        project.rename(name);
        return project;
    }

    @Transactional
    @Override
    public void deleteProject(Long userId, Long projectId) {
        Project project =
                projectPort
                        .findByIdAndUserId(projectId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        if (project.getTitleCount() > 0) {
            throw new BusinessException(ErrorCode.PROJECT_HAS_RECORDS);
        }
        project.softDelete();
    }
}
