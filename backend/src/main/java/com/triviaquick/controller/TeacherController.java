package com.triviaquick.controller;

import com.triviaquick.dto.TeacherDashboardDTO;
import com.triviaquick.security.AuthUser;
import com.triviaquick.service.TeacherDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Panel docente")
public class TeacherController {

    private final TeacherDashboardService dashboardService;

    public TeacherController(TeacherDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Resumen del panel docente (cuestionarios, partidas y resultados recientes)")
    public TeacherDashboardDTO dashboard(@AuthenticationPrincipal AuthUser teacher) {
        return dashboardService.dashboard(teacher);
    }
}
