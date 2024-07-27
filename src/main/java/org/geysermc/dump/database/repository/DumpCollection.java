package org.geysermc.dump.database.repository;

import org.bson.types.ObjectId;
import org.geysermc.dump.database.model.StoredDump;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DumpCollection extends MongoRepository<StoredDump, ObjectId> {
    Optional<StoredDump> findById(String id);

    int countById(String id);
}
