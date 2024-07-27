package org.geysermc.dump.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.geysermc.dump.KeyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class OldController {

    private final DumpController dumpController;

    @Autowired
    public OldController(DumpController dumpController) {
        this.dumpController = dumpController;
    }

    @PostMapping("/documents")
    @Deprecated
    public KeyResponse documents(HttpServletResponse response, @RequestBody String body) {
        return dumpController.submit(response, body);
    }

    @GetMapping("/raw/{dumpId:[a-zA-Z0-9]{32}}")
    @Deprecated
    public String document(HttpServletResponse response, @PathVariable String dumpId) {
        return dumpController.raw(response, dumpId);
    }
}
