package local.ims.task1.resource_service.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * End-to-End test runner.
 * Runs Cucumber scenarios from /e2e/ feature files against the real server (RANDOM_PORT).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("e2e")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "local.ims.task1.resource_service.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class E2ETestRunner {
}

