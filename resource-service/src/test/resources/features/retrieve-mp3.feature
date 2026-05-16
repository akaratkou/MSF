Feature: MP3 Resource Retrieval
  As a client application
  I want to retrieve MP3 files by ID
  So that I can download previously uploaded files

  Background:
    Given the resource service is running

  Scenario: Successfully retrieve an existing MP3 file
    Given a resource with ID 1 exists in the system
    When I request to get resource with ID "1"
    Then the response status should be 200
    And the response content type should be "audio/mpeg"
    And the response should contain MP3 binary data

  Scenario: Attempt to retrieve non-existent resource
    When I request to get resource with ID "9999"
    Then the response status should be 404
    And the response should contain error message "Resource with ID=9999 not found"

  Scenario: Attempt to retrieve resource with invalid ID
    When I request to get resource with ID "0"
    Then the response status should be 400
    And the response should contain error message "ID must be a positive integer"

  Scenario: Attempt to retrieve resource with negative ID
    When I request to get resource with ID "-5"
    Then the response status should be 400
    And the response should contain error message "ID must be a positive integer"

  Scenario: Retrieve large MP3 file
    Given a resource with ID 100 exists with size 5000000 bytes
    When I request to get resource with ID "100"
    Then the response status should be 200
    And the response should contain 5000000 bytes of data
