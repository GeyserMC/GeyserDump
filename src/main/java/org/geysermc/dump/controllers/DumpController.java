package org.geysermc.dump.controllers;

import com.google.common.hash.Hashing;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
        Optional<StoredDump> storedDump = dumpCollection.findById(Hashing.md5().hashUnencodedChars(dumpId).toString());

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

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
        Optional<StoredDump> storedDump = dumpCollection.findById(Hashing.md5().hashUnencodedChars(dumpId).toString());

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

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
        String id;
        String idHash;

        // Generate a unique id for the dump 32 characters long
        // TODO Add some checks to prevent this from being an infinite loop
        do {
            id = "";

            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            for (int i = 0; i < 32; i++) {
                id += chars.charAt((int) (Math.random() * chars.length()));
            }

            idHash = Hashing.md5().hashUnencodedChars(id).toString();
        } while (dumpCollection.countById(idHash) > 0);

        dumpCollection.insert(StoredDump.from(idHash, dumpData));

        response.setStatus(200);
        return new KeyResponse(id);
    }
}
