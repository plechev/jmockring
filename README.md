# jmockring

Jmockring is an integration testing utility for REST applications written in Java and using the Spring stack.
 
The main features of JMockring are:
 
 1. Bootstrap and deploy Java web application from within standard JUnit test - no need for maven Jetty plugin and integration 
    tests can now be executed directly from your favourite IDE.
 2. Ability to automatically mock entire layers of the application - creating very flexible approach to writing integration tests and allowing for partial-integration tests.
 3. Configure the entire web application at test level, providing the ability to tweak startup parameters or Spring context for specific scenarios.
 4. Execute tests as part of a JUnit Suite, where the application is configured at suite level and shared among all test classes. 
 5. Annotation injection for Spring beans, mocks, server configuration and a pre-configured [RestAssured] client instance   
 6. Automatic discovery of mocking libraries available on the classpath (currently only [Mockito] and [PowerMockito] are supported)




   
[RestAssured]:https://github.com/jayway/rest-assured
[Mockito]:https://github.com/mockito/mockito
[PowerMockito]:https://code.google.com/p/powermock 
