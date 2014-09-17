package org.kalipo.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.kalipo.domain.Tag;
import org.kalipo.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Tag.
 */
@RestController
@RequestMapping("/app")
public class TagResource {

    private final Logger log = LoggerFactory.getLogger(TagResource.class);

    @Inject
    private TagRepository tagRepository;

    /**
     * POST  /rest/tags -> Create a new tag.
     */
    @RequestMapping(value = "/rest/tags",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a new tag")
    public void create(@Valid @RequestBody Tag tag) {
        log.debug("REST request to save Tag : {}", tag);
        tagRepository.save(tag);
    }

    /**
     * PUT  /rest/tags -> Update existing tag.
     */
    @RequestMapping(value = "/rest/tags/{id}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @ApiOperation(value = "Update existing tag")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Tag not found")
    })
    public void update(@PathVariable String id, @Valid @RequestBody Tag tag) throws KalipoRequestException {
        log.debug("REST request to update Tag : {}", tag);

        if (StringUtils.isBlank(id)) {
            throw new InvalidParameterException("id");
        }

        tagRepository.save(tag);
    }

    /**
     * GET  /rest/tags -> get all the tags.
     */
    @RequestMapping(value = "/rest/tags",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @ApiOperation(value = "Get all the tags")
    public List<Tag> getAll() {
        log.debug("REST request to get all Tags");
        return tagRepository.findAll();
    }

    /**
     * GET  /rest/tags/:id -> get the "id" tag.
     */
    @RequestMapping(value = "/rest/tags/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @ApiOperation(value = "Get the \"id\" tag.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Tag not found")
    })
    public ResponseEntity<Tag> get(@PathVariable String id) throws KalipoRequestException {
        log.debug("REST request to get Tag : {}", id);
        if (StringUtils.isBlank(id)) {
            throw new InvalidParameterException("id");
        }

        return Optional.ofNullable(tagRepository.findOne(id))
                .map(tag -> new ResponseEntity<>(
                        tag,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /rest/tags/:id -> delete the "id" tag.
     */
    @RequestMapping(value = "/rest/tags/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Delete the \"id\" tag")
    public void delete(@PathVariable String id) throws KalipoRequestException {
        log.debug("REST request to delete Tag : {}", id);
        if (StringUtils.isBlank(id)) {
            throw new InvalidParameterException("id");
        }

        tagRepository.delete(id);
    }
}
