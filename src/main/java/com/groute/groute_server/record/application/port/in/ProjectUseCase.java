package com.groute.groute_server.record.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.groute.groute_server.record.domain.Project;

/** 프로젝트 태그 CRUD 유스케이스. */
public interface ProjectUseCase {

    Project createProject(Long userId, String name);

    Page<Project> getProjects(Long userId, Pageable pageable);

    Project updateProject(Long userId, Long projectId, String name);

    void deleteProject(Long userId, Long projectId);
}
