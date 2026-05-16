Feature: End-to-End MP3 Resource Management
  As a client system
  I want to manage MP3 resources through the Resource Service API
  So that files are uploaded, retrievable, and deletable end-to-end

  Background:
    Given the resource service API is available

  Scenario: Full lifecycle - upload, retrieve, and delete an MP3 resource
    Given I have a valid MP3 file ready for upload
    When I upload the MP3 file via POST "/resources"
    Then the upload response status is 200
    And I receive a valid resource ID in the upload response
    When I retrieve the resource using the returned ID via GET "/resources/{id}"
    Then the retrieval response status is 200
    And the response content type is "audio/mpeg"
    And the retrieved file size matches the uploaded file
    When I delete the resource using the returned ID via DELETE "/resources?id={id}"
    Then the delete response status is 200
    And the deleted IDs list contains the resource ID
    When I try to retrieve the deleted resource
    Then the response status is 404

  Scenario: Upload multiple files and bulk delete
    Given I upload 2 different MP3 files via POST "/resources"
    Then both uploads succeed with status 200
    And I receive 2 unique resource IDs
    When I delete all uploaded resources via DELETE "/resources?id={ids}"
    Then the delete response status is 200
    And both resource IDs are in the deleted IDs list
    And the resources no longer exist in the system

  Scenario: API rejects invalid inputs end-to-end
    When I send a POST to "/resources" with empty body and content-type "audio/mpeg"
    Then the response status is 400
    And the error message contains "MP3 data must not be empty"
    When I send a GET to "/resources/0"
    Then the response status is 400
    When I send a GET to "/resources/99999"
    Then the response status is 404

