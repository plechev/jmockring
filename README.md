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

## Usage 

#### Setup for a individual test class
    
    @RunWith(ExternalServerJUnitRunner.class) // <- use ExternalServerJUnitRunner for bootstrapping application in an isolated test class
    @BootstrapConfig(numberOfAttempts = 10)   // <- some server startup options 
    @Servers(value = {                        // <- declare one or more servers with the application context each one will run   
        @Server(
            dynamicContexts = @DynamicContext(  // <- @DynamicContext allows to compose the Web application via annotaions
                                                //          This tries to mimic what the web descriptor will do in real life
                springContextLocations = "classpath:/spring/application-context.xml",
                contextPath = "/context1",
                servlets = {@Servlet(servletClass = DispatcherServlet.class, params = {@Param(name = "contextConfigLocation", value = "classpath:/spring/web-mvc-context.xml")})},
                excludedConfigLocations = "repository-context.xml"  // <- filename patterns to exclude when building the Spring context
                                                                    //    Such exclusions allow partial Spring context to be deployed with any "missing" beans being auto-mocked
                                                                    //    Useful, if the test does not care about particular part of the application and would benefit from stubbing it instead.                                                                                          
            ),
            bootstrap = JettyWebServer.class,                       // <- deploy on embedded Jetty server (Tomcat support is WIP)  
            name = "ex1",                                           // <- identify this particular server - used when injecting various objects into the test   
            testClass = JettyServerSuiteIT.class   
        )
        ,
        @Server(                             // <- Multiple servers can be started with same or different context configuration 
                                             //    The PortChecker will ensure TCP ports for the running HTTP listener are automatically allocated based on availability  
            dynamicContexts = @DynamicContext(
                springContextLocations = "classpath:/spring/application-context.xml",
                contextPath = "/context2",
                servlets = {@Servlet(servletClass = DispatcherServlet.class, params = {@Param(name = "contextConfigLocation", value = "classpath:/spring/web-mvc-context.xml")})}
            ),
            propertiesLocation = "/mock/base-webserver-ri.properties",
            bootstrap = JettyWebServer.class,
            name = "ex2",
            testClass = JettyServerSuiteIT.class)
    })
    public class MyRESTIntegrationIT {
        
        @RequestClient(executionName = "ex1", contextPath = "/context1")  // <- inject the RestAssured wrapper for server configuration "ex1"  
        private RestAssuredClient client1;
        
        @RequestClient(executionName = "ex2", contextPath = "/context2")  // <- inject the RestAssured wrapper for server configuration "ex2"  
        private RestAssuredClient client2;
        
        @RemoteMock(executionName = "ex1", contextPath = "/context1")     // <- inject the auto-mocked repository from sserver "ex1"
        private MyRepository myRepositoryMock;
        
        @Test
        public void shouldExecuteRESTCallAndVerifyResponse() throws Exception {
    
            // RECORD REMOTE MOCKS BEHAVIOUR >>
            Mockito.when(myRepositoryMock.getValue(eq("queryValue"))).thenReturn("Expected Value");
    
            // execute and verify REST 
            client1.newRequest()  // <- calling `newRequest()` on the wrapper returns the pre-configured (ready to use) RestAssured specification
                                  //    from this point on, the control is passed to RestAssured's DSL builder.   
                .request().log().all(true)
                .response().log().all(true)
                .expect()
                .statusCode(200)
                .content("value", is("Expected Value"))
                .when()
                .get("/app-path-excluding-context-path/{query}", "queryValue");

        }
        
        ... 
    }


## Limitations
 
 * Currently the support for deploying the application from `web.xml` descriptor does not work
 * The application bootstrap requires using either `ExternalServerJUnitRunner` or `ExternalServerJUnitSuiteRunner` thus preventing usage of any other JNit runners. 
 * Spring auto-mocking works best for well layered physical designs, i.e. separate XML configuration files for rest, service, repository, etc ... layers
 * No current support for applications using static assets, JSPs or similar technologies. The best use case for __jmockring__ is a _pure_ REST application.   
         
## TODO
    
 * Add support for `web.xml`
 * Use javassist for bytecode rewrite to auto-weave Spring context loaders (required for full `web.xml` support)
 * Add support for static assets
 * Move bootstrapping to use JUnit rules instead of runners, thus freeing the test class to use a runner of choice
    
