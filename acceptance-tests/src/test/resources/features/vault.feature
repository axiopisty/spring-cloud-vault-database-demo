Feature: Vault Integration

  Scenario: The application should be able to load secrets from a Vault generic secrets backend
    When an http GET request is issued to /api/secrets
    Then the response status should be 200
    And the response body should be {"someSecret":"Example secret retrieved from Vault!"}