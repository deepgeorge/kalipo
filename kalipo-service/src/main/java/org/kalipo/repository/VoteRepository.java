package org.kalipo.repository;

import org.joda.time.DateTime;
import org.kalipo.domain.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Spring Data MongoDB repository for the Vote entity.
 */
public interface VoteRepository extends MongoRepository<Vote, String> {

    @Query(value = "{'authorId': ?0, 'createdDate': {$gte: ?1, $lt: ?2}}", count = true)
    int countWithinDateRange(String currentLogin, DateTime from, DateTime to);
}
