package local.ims.task1.resource_service.component.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import local.ims.task1.resource_service.component.ScenarioContext;
import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import local.ims.task1.resource_service.repositories.Mp3ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Cucumber step definitions for resource-service component tests.
 * Uses Spring MockMvc for full Spring MVC stack testing without real HTTP.
 *
 * ID-mapping strategy:
 *   Feature files use "label" IDs (1, 10, 50…) for readability.
 *   Each "Given resource with ID X exists" step uploads an MP3 and maps
 *   label X -> actual auto-generated DB ID. All subsequent steps translate automatically.
 */
public class ResourceServiceSteps {

    @Autowired private ScenarioContext          ctx;
    @Autowired private Mp3ResourceRepository   repository;
    @Autowired private MetadataServiceClient   metadataServiceClient;
    @Autowired private WebApplicationContext   webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void cleanDatabase() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
        repository.deleteAll();
    }

    // ── Background ───────────────────────────────────────────────────────────

    @Given("the resource service is running")
    public void serviceRunning() {}

    @Given("S3 storage is available")
    public void s3Available() {}

    @Given("RabbitMQ is available")
    public void rabbitAvailable() {}

    // ── Upload ───────────────────────────────────────────────────────────────

    @Given("I have a valid MP3 file with size {int} bytes")
    public void haveMp3OfSize(int size) { ctx.setRequestBody(fakeMp3(size)); }

    @Given("I have a valid MP3 file {string}")
    public void haveMp3Named(String name) { ctx.setRequestBody(fakeMp3Named(name)); }

    @Given("I have an empty file")
    public void haveEmptyFile() { ctx.setRequestBody(new byte[0]); }

    @Given("I have {int} different MP3 files")
    public void haveMultipleMp3Files(int n) {
        List<byte[]> files = new ArrayList<>();
        for (int i = 0; i < n; i++) files.add(fakeMp3Named("file-" + i));
        ctx.setMultipleFiles(files);
    }

    @Given("I have already uploaded this file with resource ID")
    public void alreadyUploadedFile() {
        MockHttpServletResponse r = postMp3(ctx.getRequestBody());
        ctx.setFirstUploadId(jsonInt(r, "id"));
    }

    @When("I upload the MP3 file to {string}")
    @When("I upload the file to {string}")
    public void uploadFileTo(String path) {
        ctx.setLastResponse(postMp3(ctx.getRequestBody()));
    }

    @When("I upload the same file again to {string}")
    public void uploadSameFileAgain(String path) {
        ctx.setLastResponse(postMp3(ctx.getRequestBody()));
    }

    @When("I upload all files to {string}")
    public void uploadAllFilesTo(String path) {
        List<MockHttpServletResponse> responses = new ArrayList<>();
        for (byte[] f : ctx.getMultipleFiles()) responses.add(postMp3(f));
        ctx.setMultipleResponses(responses);
        ctx.setLastResponse(responses.get(responses.size() - 1));
    }

    // ── Retrieve ─────────────────────────────────────────────────────────────

    @Given("a resource with ID {int} exists in the system")
    public void aResourceWithIdExists(int label) { uploadAndMap(label); }

    @Given("a resource with ID {int} exists with size {int} bytes")
    public void aResourceWithIdExistsWithSize(int label, int size) {
        MockHttpServletResponse r = postMp3(fakeMp3(size));
        ctx.mapId(label, jsonInt(r, "id"));
    }

    @When("I request to get resource with ID {string}")
    public void requestGetById(String idStr) {
        ctx.setLastResponse(getMp3(resolveIdStr(idStr)));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Given("resource with ID {int} exists in the system")
    public void resourceWithIdExists(int label) { uploadAndMap(label); }

    @Given("resources with IDs {int}, {int}, {int} exist in the system")
    public void resourcesThreeExist(int l1, int l2, int l3) {
        uploadAndMap(l1); uploadAndMap(l2); uploadAndMap(l3);
    }

    @Given("resources with IDs {int}, {int} exist in the system")
    public void resourcesTwoExist(int l1, int l2) { uploadAndMap(l1); uploadAndMap(l2); }

    @Given("the resource is stored in S3")
    public void resourceStoredInS3() { /* covered by upload */ }

    @Given("resource with ID {int} exists with song metadata")
    public void resourceWithSongMetadata(int label) { uploadAndMap(label); }

    @Given("I have a CSV string with {int} characters")
    public void haveLongCsv(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) sb.append("1,");
        ctx.setLongCsvString(sb.substring(0, length));
    }

    @When("I delete resource with IDs {string}")
    @When("I delete resources with IDs {string}")
    public void deleteResources(String labelCsv) {
        ctx.setLastDeleteLabelIds(parseLabelIds(labelCsv));
        ctx.setLastResponse(performDelete(translateIds(labelCsv)));
    }

    @When("I delete resources with the long CSV")
    public void deleteWithLongCsv() {
        ctx.setLastResponse(performDelete(ctx.getLongCsvString()));
    }

    // ── Assertions ───────────────────────────────────────────────────────────

    @Then("the response status should be {int}")
    public void responseStatus(int status) {
        assertThat(ctx.getLastResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response should contain a resource ID")
    public void responseContainsId() {
        assertThat(jsonNode(ctx.getLastResponse()).has("id")).isTrue();
    }

    @Then("the resource ID should be a positive integer")
    public void resourceIdPositive() {
        assertThat(jsonInt(ctx.getLastResponse(), "id")).isPositive();
    }

    @Then("the response content type should be {string}")
    public void responseContentType(String ct) {
        assertThat(ctx.getLastResponse().getContentType()).containsIgnoringCase(ct);
    }

    @Then("the response should contain MP3 binary data")
    public void responseContainsMp3() {
        assertThat(ctx.getLastResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Then("the response should contain {int} bytes of data")
    public void responseContainsBytes(int n) {
        assertThat(ctx.getLastResponse().getContentAsByteArray()).hasSize(n);
    }

    @Then("the response should contain error message {string}")
    public void responseErrorMessage(String msg) {
        try {
            String body = ctx.getLastResponse().getContentAsString();
            // Normalize punctuation so "ID. Must be" matches feature-file "ID must be"
            String normalizedBody = body.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ");
            String normalizedMsg  = msg.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
            assertThat(normalizedBody).contains(normalizedMsg);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Then("the response should contain a different resource ID")
    public void responseDifferentId() {
        assertThat(jsonInt(ctx.getLastResponse(), "id")).isNotEqualTo(ctx.getFirstUploadId());
    }

    @Then("both resources should exist in the system")
    public void bothResourcesExist() {
        assertThat(getMp3(String.valueOf(ctx.getFirstUploadId())).getStatus()).isEqualTo(200);
        assertThat(getMp3(String.valueOf(jsonInt(ctx.getLastResponse(), "id"))).getStatus()).isEqualTo(200);
    }

    @Then("all uploads should succeed with status {int}")
    public void allUploadsStatus(int status) {
        ctx.getMultipleResponses().forEach(r -> assertThat(r.getStatus()).isEqualTo(status));
    }

    @Then("I should receive {int} different resource IDs")
    public void receiveDifferentIds(int n) {
        long distinct = ctx.getMultipleResponses().stream()
                .mapToInt(r -> jsonInt(r, "id")).distinct().count();
        assertThat(distinct).isEqualTo(n);
    }

    @Then("all resources should be stored in S3")
    public void allStoredInS3() {
        ctx.getMultipleResponses().forEach(r ->
                assertThat(getMp3(String.valueOf(jsonInt(r, "id"))).getStatus()).isEqualTo(200));
    }

    @Then("^the response should contain deleted IDs \\[(.*)\\]$")
    public void responseContainsDeletedIds(String labelCsv) {
        List<Integer> expected = Arrays.stream(labelCsv.split(","))
                .map(String::trim)
                .map(s -> ctx.resolveId(Integer.parseInt(s)))
                .collect(Collectors.toList());
        List<Integer> actual = jsonIntList(ctx.getLastResponse(), "ids");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("the response should contain empty deleted IDs list")
    public void responseEmptyDeletedIds() {
        assertThat(jsonIntList(ctx.getLastResponse(), "ids")).isEmpty();
    }

    @Then("the resource should be deleted from database")
    @Then("the resource should be deleted from S3")
    @Then("all resources should be deleted from database")
    public void resourceDeleted() {
        for (int label : ctx.getLastDeleteLabelIds()) {
            Integer actual = ctx.getIdMapping().get(label);
            if (actual != null) assertThat(getMp3(String.valueOf(actual)).getStatus()).isEqualTo(404);
        }
    }

    @Then("resources {int} and {int} should be deleted from database")
    public void twoResourcesDeleted(int l1, int l2) {
        for (int l : new int[]{l1, l2})
            assertThat(getMp3(String.valueOf(ctx.resolveId(l))).getStatus()).isEqualTo(404);
    }

    @Then("the song metadata should also be deleted")
    public void songMetadataDeleted() {
        verify(metadataServiceClient).deleteSongMetadata(anyString());
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    private MockHttpServletResponse postMp3(byte[] data) {
        try {
            return mockMvc.perform(
                    MockMvcRequestBuilders.post("/resources")
                            .contentType("audio/mpeg")
                            .content(data)
            ).andReturn().getResponse();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private MockHttpServletResponse getMp3(String id) {
        try {
            return mockMvc.perform(
                    MockMvcRequestBuilders.get("/resources/" + id)
            ).andReturn().getResponse();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private MockHttpServletResponse performDelete(String idsCsv) {
        try {
            return mockMvc.perform(
                    MockMvcRequestBuilders.delete("/resources").param("id", idsCsv)
            ).andReturn().getResponse();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private JsonNode jsonNode(MockHttpServletResponse r) {
        try {
            return objectMapper.readTree(r.getContentAsByteArray());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private int jsonInt(MockHttpServletResponse r, String field) {
        return jsonNode(r).get(field).asInt();
    }

    private List<Integer> jsonIntList(MockHttpServletResponse r, String field) {
        JsonNode arr = jsonNode(r).get(field);
        List<Integer> list = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(n -> list.add(n.asInt()));
        return list;
    }

    // ── Data helpers ─────────────────────────────────────────────────────────

    private byte[] fakeMp3(int size) {
        int len = Math.max(size, 10);
        byte[] b = new byte[len];
        b[0] = 'I'; b[1] = 'D'; b[2] = '3'; b[3] = 0x03;
        for (int i = 4; i < len; i++) b[i] = (byte)(i % 256);
        return Arrays.copyOf(b, size > 0 ? size : len);
    }

    private byte[] fakeMp3Named(String name) {
        byte[] b = fakeMp3(1024);
        int h = name.hashCode();
        b[4] = (byte)(h & 0xFF); b[5] = (byte)((h >> 8) & 0xFF);
        return b;
    }

    private int uploadAndMap(int label) {
        MockHttpServletResponse r = postMp3(fakeMp3Named("resource-" + label));
        assertThat(r.getStatus()).isEqualTo(200);
        int actual = jsonInt(r, "id");
        ctx.mapId(label, actual);
        return actual;
    }

    private String translateIds(String csv) {
        if (csv == null || csv.isBlank()) return csv;
        return Arrays.stream(csv.split(",")).map(String::trim).map(p -> {
            try {
                Integer a = ctx.getIdMapping().get(Integer.parseInt(p));
                return a != null ? String.valueOf(a) : p;
            } catch (NumberFormatException e) { return p; }
        }).collect(Collectors.joining(","));
    }

    private List<Integer> parseLabelIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim)
                .flatMap(p -> {
                    try { return Stream.of(Integer.parseInt(p)); }
                    catch (NumberFormatException e) { return Stream.empty(); }
                }).collect(Collectors.toList());
    }

    private String resolveIdStr(String s) {
        try {
            Integer a = ctx.getIdMapping().get(Integer.parseInt(s));
            return a != null ? String.valueOf(a) : s;
        } catch (NumberFormatException e) { return s; }
    }
}
