package org.geysermc.dump.controllers;

import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletResponse;
import org.geysermc.dump.AnalysedDump;
import org.geysermc.dump.KeyResponse;
import org.geysermc.dump.database.model.StoredDump;
import org.geysermc.dump.database.repository.DumpCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class DumpController {

    private final DumpCollection dumpCollection;

    @Autowired
    public DumpController(DumpCollection dumpCollection) {
        this.dumpCollection = dumpCollection;
    }

    @GetMapping("/raw/{dumpId:[a-zA-Z0-9]{32}}")
    public String raw(HttpServletResponse response, @PathVariable String dumpId) {
        Optional<StoredDump> storedDump = dumpCollection.findById(Hashing.md5().hashUnencodedChars(dumpId).toString());

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

        response.setStatus(200);
        return storedDump.get().raw();
    }

    @GetMapping("/analysed/{dumpId:[a-zA-Z0-9]{32}}")
    public AnalysedDump analysed(HttpServletResponse response, @PathVariable String dumpId) {
        Optional<StoredDump> storedDump = dumpCollection.findById(Hashing.md5().hashUnencodedChars(dumpId).toString());

        if (storedDump.isEmpty()) {
            response.setStatus(404);
            return null;
        }

        response.setStatus(200);
        return AnalysedDump.from(storedDump.get());
    }

    @PostMapping("/submit")
    public KeyResponse submit(HttpServletResponse response, @RequestBody String dumpData) {
        response.setStatus(200);

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

        return new KeyResponse(id);
    }
}
