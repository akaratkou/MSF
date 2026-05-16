Feature: MP3 Resource Upload
  As a client application
  I want to upload MP3 files
  So that they can be stored and processed for metadata extraction

  Background:
    Given the resource service is running
    And S3 storage is available
    And RabbitMQ is available

  Scenario: Successfully upload a valid MP3 file
    Given I have a valid MP3 file with size 1024 bytes
    When I upload the MP3 file to "/resources"
    Then the response status should be 200
    And the response should contain a resource ID
    And the resource ID should be a positive integer

  Scenario: Reject upload of empty file
    Given I have an empty file
    When I upload the file to "/resources"
    Then the response status should be 400
    And the response should contain error message "MP3 data must not be empty"

  Scenario: Upload duplicate MP3 file should create new resource
    Given I have a valid MP3 file "test-song.mp3"
    And I have already uploaded this file with resource ID
    When I upload the same file again to "/resources"
    Then the response status should be 200
    And the response should contain a different resource ID
    And both resources should exist in the system

  Scenario: Successfully upload multiple MP3 files
    Given I have 3 different MP3 files
    When I upload all files to "/resources"
    Then all uploads should succeed with status 200
    And I should receive 3 different resource IDs
    And all resources should be stored in S3
