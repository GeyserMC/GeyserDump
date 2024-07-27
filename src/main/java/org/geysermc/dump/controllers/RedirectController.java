package org.geysermc.dump.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {
    @Operation(summary = "Redirect to the dump viewer")
    @ApiResponse(responseCode = "302", description = "Redirect to the dump viewer")
    @GetMapping("/{dumpId:[a-zA-Z0-9]{32}}")
    public void dump(HttpServletResponse response, @PathVariable String dumpId) throws IOException {
        response.sendRedirect("https://geysermc.org/utilities/dump-viewer#" + dumpId);
    }

    @Hidden
    @GetMapping("/")
    public void dump(HttpServletResponse response) throws IOException {
        response.sendRedirect("https://geysermc.org/utilities/dump-viewer");
    }
}
