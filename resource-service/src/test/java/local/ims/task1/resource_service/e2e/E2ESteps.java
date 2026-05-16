package local.ims.task1.resource_service.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for E2E API scenarios.
 * Uses MockMvc directly to avoid rest-assured/Spring MVC 7 compatibility issues.
 */
public class E2ESteps {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // ---- scenario state ----
    private byte[]                     uploadedBytes;
    private MockHttpServletResponse    lastResponse;
    private Integer                    lastUploadedId;
    private final List<Integer>        uploadedIds     = new ArrayList<>();
    private final List<MockHttpServletResponse> uploadResponses = new ArrayList<>();

    private static final byte[] SAMPLE_MP3 = new byte[1024];

    @Before
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void cleanup() {
        uploadedIds.clear();
        uploadResponses.clear();
        lastResponse   = null;
        lastUploadedId = null;
        uploadedBytes  = null;
    }

    // ──────────────────────────────────────────────
    //  Background
    // ──────────────────────────────────────────────

    @Given("the resource service API is available")
    public void serviceIsAvailable() {
        assertThat(mockMvc).isNotNull();
    }

    // ──────────────────────────────────────────────
    //  Upload
    // ──────────────────────────────────────────────

    @Given("I have a valid MP3 file ready for upload")
    public void haveValidMp3() {
        uploadedBytes = SAMPLE_MP3.clone();
    }

    @When("I upload the MP3 file via POST {string}")
    public void uploadMp3(String path) throws Exception {
        lastResponse = mockMvc.perform(
                MockMvcRequestBuilders.post(path)
                        .contentType("audio/mpeg")
                        .content(uploadedBytes))
                .andReturn().getResponse();
        lastUploadedId = extractId(lastResponse);
        uploadedIds.add(lastUploadedId);
    }

    @Given("I upload {int} different MP3 files via POST {string}")
    public void uploadMultipleFiles(int count, String path) throws Exception {
        for (int i = 0; i < count; i++) {
            byte[] bytes = new byte[1024 + i * 100];
            Arrays.fill(bytes, (byte) (i + 1));
            MockHttpServletResponse r = mockMvc.perform(
                    MockMvcRequestBuilders.post(path)
                            .contentType("audio/mpeg")
                            .content(bytes))
                    .andReturn().getResponse();
            uploadResponses.add(r);
            uploadedIds.add(extractId(r));
        }
        lastResponse = uploadResponses.get(uploadResponses.size() - 1);
    }

    // ──────────────────────────────────────────────
    //  Retrieve
    // ──────────────────────────────────────────────

    @When("I retrieve the resource using the returned ID via GET {string}")
    public void retrieveById(String pathTemplate) throws Exception {
        String path = pathTemplate.replace("{id}", String.valueOf(lastUploadedId));
        lastResponse = mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andReturn().getResponse();
    }

    @When("I try to retrieve the deleted resource")
    public void retrieveDeletedResource() throws Exception {
        lastResponse = mockMvc.perform(MockMvcRequestBuilders.get("/resources/" + lastUploadedId))
                .andReturn().getResponse();
    }

    @When("I send a GET to {string}")
    public void sendGet(String path) throws Exception {
        lastResponse = mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andReturn().getResponse();
    }

    // ──────────────────────────────────────────────
    //  Delete
    // ──────────────────────────────────────────────

    @When("I delete the resource using the returned ID via DELETE {string}")
    public void deleteById(String pathTemplate) throws Exception {
        String path = pathTemplate.replace("{id}", String.valueOf(lastUploadedId));
        lastResponse = mockMvc.perform(MockMvcRequestBuilders.delete(path))
                .andReturn().getResponse();
    }

    @When("I delete all uploaded resources via DELETE {string}")
    public void deleteAll(String pathTemplate) throws Exception {
        String ids = uploadedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String path = pathTemplate.replace("{ids}", ids);
        lastResponse = mockMvc.perform(MockMvcRequestBuilders.delete(path))
                .andReturn().getResponse();
    }

    // ──────────────────────────────────────────────
    //  Invalid input
    // ──────────────────────────────────────────────

    @When("I send a POST to {string} with empty body and content-type {string}")
    public void sendPostEmptyBody(String path, String contentType) throws Exception {
        lastResponse = mockMvc.perform(
                MockMvcRequestBuilders.post(path)
                        .contentType(contentType)
                        .content(new byte[0]))
                .andReturn().getResponse();
    }

    // ──────────────────────────────────────────────
    //  Assertions
    // ──────────────────────────────────────────────

    @Then("the upload response status is {int}")
    public void uploadStatus(int expected) {
        assertThat(lastResponse.getStatus()).isEqualTo(expected);
    }

    @Then("both uploads succeed with status {int}")
    public void bothUploadsStatus(int expected) {
        uploadResponses.forEach(r -> assertThat(r.getStatus()).isEqualTo(expected));
    }

    @And("I receive a valid resource ID in the upload response")
    public void receiveValidId() {
        assertThat(lastUploadedId).isNotNull().isPositive();
    }

    @And("I receive {int} unique resource IDs")
    public void receiveUniqueIds(int count) {
        assertThat(uploadedIds).hasSize(count);
        assertThat(uploadedIds.stream().distinct().count()).isEqualTo(count);
    }

    @Then("the retrieval response status is {int}")
    public void retrievalStatus(int expected) {
        assertThat(lastResponse.getStatus()).isEqualTo(expected);
    }

    @And("the response content type is {string}")
    public void checkContentType(String expected) {
        assertThat(lastResponse.getContentType()).contains(expected);
    }

    @And("the retrieved file size matches the uploaded file")
    public void checkFileSize() {
        assertThat(lastResponse.getContentAsByteArray().length).isEqualTo(uploadedBytes.length);
    }

    @Then("the delete response status is {int}")
    public void deleteStatus(int expected) {
        assertThat(lastResponse.getStatus()).isEqualTo(expected);
    }

    @And("the deleted IDs list contains the resource ID")
    public void deletedIdsContainId() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(lastResponse.getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        assertThat(ids).contains(lastUploadedId);
    }

    @And("both resource IDs are in the deleted IDs list")
    public void bothIdsInDeletedList() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(lastResponse.getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        assertThat(ids).containsExactlyInAnyOrderElementsOf(uploadedIds);
    }

    @And("the resources no longer exist in the system")
    public void resourcesNoLongerExist() throws Exception {
        for (Integer id : uploadedIds) {
            MockHttpServletResponse r = mockMvc.perform(MockMvcRequestBuilders.get("/resources/" + id))
                    .andReturn().getResponse();
            assertThat(r.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Then("the response status is {int}")
    public void responseStatus(int expected) {
        assertThat(lastResponse.getStatus()).isEqualTo(expected);
    }

    @And("the error message contains {string}")
    public void errorMessageContains(String text) throws Exception {
        assertThat(lastResponse.getContentAsString()).contains(text);
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────

    private Integer extractId(MockHttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        return (Integer) body.get("id");
    }
}
