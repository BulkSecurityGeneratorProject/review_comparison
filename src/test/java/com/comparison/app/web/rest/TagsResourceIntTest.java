package com.comparison.app.web.rest;

import com.comparison.app.ReviewComparisonApp;

import com.comparison.app.domain.Tags;
import com.comparison.app.repository.TagsRepository;
import com.comparison.app.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


import static com.comparison.app.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the TagsResource REST controller.
 *
 * @see TagsResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ReviewComparisonApp.class)
public class TagsResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_CLEATED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLEATED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private TagsRepository tagsRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restTagsMockMvc;

    private Tags tags;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final TagsResource tagsResource = new TagsResource(tagsRepository);
        this.restTagsMockMvc = MockMvcBuilders.standaloneSetup(tagsResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Tags createEntity(EntityManager em) {
        Tags tags = new Tags()
            .name(DEFAULT_NAME)
            .cleated(DEFAULT_CLEATED)
            .updated(DEFAULT_UPDATED);
        return tags;
    }

    @Before
    public void initTest() {
        tags = createEntity(em);
    }

    @Test
    @Transactional
    public void createTags() throws Exception {
        int databaseSizeBeforeCreate = tagsRepository.findAll().size();

        // Create the Tags
        restTagsMockMvc.perform(post("/api/tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tags)))
            .andExpect(status().isCreated());

        // Validate the Tags in the database
        List<Tags> tagsList = tagsRepository.findAll();
        assertThat(tagsList).hasSize(databaseSizeBeforeCreate + 1);
        Tags testTags = tagsList.get(tagsList.size() - 1);
        assertThat(testTags.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testTags.getCleated()).isEqualTo(DEFAULT_CLEATED);
        assertThat(testTags.getUpdated()).isEqualTo(DEFAULT_UPDATED);
    }

    @Test
    @Transactional
    public void createTagsWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = tagsRepository.findAll().size();

        // Create the Tags with an existing ID
        tags.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTagsMockMvc.perform(post("/api/tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tags)))
            .andExpect(status().isBadRequest());

        // Validate the Tags in the database
        List<Tags> tagsList = tagsRepository.findAll();
        assertThat(tagsList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllTags() throws Exception {
        // Initialize the database
        tagsRepository.saveAndFlush(tags);

        // Get all the tagsList
        restTagsMockMvc.perform(get("/api/tags?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tags.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].cleated").value(hasItem(DEFAULT_CLEATED.toString())))
            .andExpect(jsonPath("$.[*].updated").value(hasItem(DEFAULT_UPDATED.toString())));
    }
    
    @Test
    @Transactional
    public void getTags() throws Exception {
        // Initialize the database
        tagsRepository.saveAndFlush(tags);

        // Get the tags
        restTagsMockMvc.perform(get("/api/tags/{id}", tags.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(tags.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.cleated").value(DEFAULT_CLEATED.toString()))
            .andExpect(jsonPath("$.updated").value(DEFAULT_UPDATED.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingTags() throws Exception {
        // Get the tags
        restTagsMockMvc.perform(get("/api/tags/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTags() throws Exception {
        // Initialize the database
        tagsRepository.saveAndFlush(tags);

        int databaseSizeBeforeUpdate = tagsRepository.findAll().size();

        // Update the tags
        Tags updatedTags = tagsRepository.findById(tags.getId()).get();
        // Disconnect from session so that the updates on updatedTags are not directly saved in db
        em.detach(updatedTags);
        updatedTags
            .name(UPDATED_NAME)
            .cleated(UPDATED_CLEATED)
            .updated(UPDATED_UPDATED);

        restTagsMockMvc.perform(put("/api/tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedTags)))
            .andExpect(status().isOk());

        // Validate the Tags in the database
        List<Tags> tagsList = tagsRepository.findAll();
        assertThat(tagsList).hasSize(databaseSizeBeforeUpdate);
        Tags testTags = tagsList.get(tagsList.size() - 1);
        assertThat(testTags.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testTags.getCleated()).isEqualTo(UPDATED_CLEATED);
        assertThat(testTags.getUpdated()).isEqualTo(UPDATED_UPDATED);
    }

    @Test
    @Transactional
    public void updateNonExistingTags() throws Exception {
        int databaseSizeBeforeUpdate = tagsRepository.findAll().size();

        // Create the Tags

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTagsMockMvc.perform(put("/api/tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tags)))
            .andExpect(status().isBadRequest());

        // Validate the Tags in the database
        List<Tags> tagsList = tagsRepository.findAll();
        assertThat(tagsList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteTags() throws Exception {
        // Initialize the database
        tagsRepository.saveAndFlush(tags);

        int databaseSizeBeforeDelete = tagsRepository.findAll().size();

        // Delete the tags
        restTagsMockMvc.perform(delete("/api/tags/{id}", tags.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Tags> tagsList = tagsRepository.findAll();
        assertThat(tagsList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tags.class);
        Tags tags1 = new Tags();
        tags1.setId(1L);
        Tags tags2 = new Tags();
        tags2.setId(tags1.getId());
        assertThat(tags1).isEqualTo(tags2);
        tags2.setId(2L);
        assertThat(tags1).isNotEqualTo(tags2);
        tags1.setId(null);
        assertThat(tags1).isNotEqualTo(tags2);
    }
}
