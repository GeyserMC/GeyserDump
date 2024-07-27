package org.geysermc.dump.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {
    @GetMapping("/{dumpId:[a-zA-Z0-9]{32}}")
    public void dump(HttpServletResponse response, @PathVariable String dumpId) throws IOException {
        response.sendRedirect("https://geysermc.org/utilities/dump-viewer#" + dumpId);
    }
}
