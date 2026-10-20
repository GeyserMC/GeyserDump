/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDump
 */

package org.geysermc.dump.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import org.geysermc.dump.AnalysedDump;
import org.geysermc.dump.KeyResponse;
import org.geysermc.dump.database.model.StoredDump;
import org.geysermc.dump.database.repository.DumpCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(value = "/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class DumpController {

    private final DumpCollection dumpCollection;

    @Autowired
    public DumpController(DumpCollection dumpCollection) {
        this.dumpCollection = dumpCollection;
    }

    @Operation(summary = "Get the raw dump data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The raw dump data"),
        @ApiResponse(responseCode = "404", description = "The dump was not found")
    })
    @GetMapping(value = "/raw/{dumpId:[a-zA-Z0-9]{32}}")
    public String raw(
        HttpServletResponse response,
        @Parameter(description = "The id of the dump to get")
        @Pattern(regexp = "[a-zA-Z0-9]{32}")
        @PathVariable
        String dumpId
    ) {
        Optional<StoredDump> storedDump = dumpCollection.findByKey(dumpId);

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

        dumpCollection.markAccessed(storedDump.get().id());

        response.setStatus(200);
        return storedDump.get().raw();
    }

    @Operation(summary = "Get the analysed dump data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The analysed dump data"),
        @ApiResponse(responseCode = "404", description = "The dump was not found")
    })
    @GetMapping("/analysed/{dumpId:[a-zA-Z0-9]{32}}")
    public AnalysedDump analysed(
        HttpServletResponse response,
        @Parameter(description = "The id of the dump to get")
        @Pattern(regexp = "[a-zA-Z0-9]{32}")
        @PathVariable
        String dumpId
    ) {
        Optional<StoredDump> storedDump = dumpCollection.findByKey(dumpId);

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

        dumpCollection.markAccessed(storedDump.get().id());

        response.setStatus(200);
        return AnalysedDump.from(storedDump.get());
    }

    @Operation(summary = "Submit a new dump")
    @PostMapping("/submit")
    public KeyResponse submit(
        HttpServletResponse response,
        @Parameter(description = "The dump data to submit")
        @RequestBody
        String dumpData
    ) {
        StoredDump storedDump = dumpCollection.save(StoredDump.from(dumpData));

        response.setStatus(200);
        return new KeyResponse(storedDump.id());
    }
}
