Feature: MP3 Resource Deletion
  As a client application
  I want to delete MP3 resources
  So that I can manage storage and cleanup unused files

  Background:
    Given the resource service is running

  Scenario: Delete single resource
    Given resource with ID 1 exists in the system
    And the resource is stored in S3
    When I delete resource with IDs "1"
    Then the response status should be 200
    And the response should contain deleted IDs [1]
    And the resource should be deleted from database
    And the resource should be deleted from S3

  Scenario: Delete multiple resources
    Given resources with IDs 1, 2, 3 exist in the system
    When I delete resources with IDs "1,2,3"
    Then the response status should be 200
    And the response should contain deleted IDs [1, 2, 3]
    And all resources should be deleted from database

  Scenario: Attempt to delete non-existent resource
    When I delete resources with IDs "9999"
    Then the response status should be 200
    And the response should contain empty deleted IDs list

  Scenario: Delete mix of existent and non-existent resources
    Given resources with IDs 10, 20 exist in the system
    When I delete resources with IDs "10,9999,20"
    Then the response status should be 200
    And the response should contain deleted IDs [10, 20]
    And resources 10 and 20 should be deleted from database

  Scenario: Invalid ID format in delete request
    When I delete resources with IDs "1,abc,3"
    Then the response status should be 400
    And the response should contain error message "Invalid ID format: 'abc'"

  Scenario: CSV exceeds maximum length
    Given I have a CSV string with 201 characters
    When I delete resources with the long CSV
    Then the response status should be 400
    And the response should contain error message "CSV string is too long"

  Scenario: Delete with empty ID parameter
    When I delete resources with IDs ""
    Then the response status should be 200
    And the response should contain empty deleted IDs list

  Scenario: Cascading delete with song metadata
    Given resource with ID 50 exists with song metadata
    When I delete resources with IDs "50"
    Then the response status should be 200
    And the response should contain deleted IDs [50]
    And the song metadata should also be deleted
