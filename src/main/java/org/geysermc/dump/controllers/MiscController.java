package org.geysermc.dump.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MiscController {
    @Operation(summary = "Simple server online check")
    @ApiResponse(responseCode = "204", description = "Server is online")
    @GetMapping("/health")
    public void health(HttpServletResponse response) {
        response.setStatus(204);
    }
}
