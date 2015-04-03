package com.serviceenabled.dropwizardrequesttracker.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.net.URI;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.serviceenabled.dropwizardrequesttracker.RequestTrackerClientFilter;
import com.serviceenabled.dropwizardrequesttracker.RequestTrackerConfiguration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


public class BundleApplicationIT {
	@ClassRule
	public static final DropwizardAppRule<BundleConfiguration> RULE = new DropwizardAppRule<BundleConfiguration>(BundleApplication.class, null);
	private static final Client CLIENT = new Client();
	private static ClientTestResource CLIENT_TEST;
	private static MockTestResource MOCK_TEST;
	private static URI INITIAL_URI;
	private static RequestTrackerConfiguration CONFIGURATION;

	@BeforeClass
	public static void setupApplication() throws Exception {
		CONFIGURATION = RULE.getConfiguration().getRequestTrackerConfiguration();
		CLIENT.addFilter(new RequestTrackerClientFilter(CONFIGURATION));

		INITIAL_URI = new URI("HTTP",null,"localhost",RULE.getLocalPort(),"/client-test",null,null);
		URI secondaryURI = new URI("HTTP",null,"localhost",RULE.getLocalPort(),"/mock-test",null,null);

		CLIENT_TEST = new ClientTestResource(secondaryURI, CLIENT);
		MOCK_TEST = new MockTestResource();

		RULE.getEnvironment().getApplicationContext().stop();
		RULE.getEnvironment().jersey().register(MOCK_TEST);
		RULE.getEnvironment().jersey().register(CLIENT_TEST);
		RULE.getEnvironment().getApplicationContext().start();
	}
	
	@Test
	public void bundleAddsFilter(){
		Environment e = RULE.getEnvironment();
		assertThat(e.getApplicationContext().getServletHandler().getFilter("request-tracker-servlet-filter"), notNullValue());
	}

	@Test
	public void addsTrackerToOutgoingRequest() throws Exception {
		CLIENT.resource(INITIAL_URI).post(ClientResponse.class);

		assertThat(MOCK_TEST.getRequestTrackerId(), notNullValue());
	}

	@Test
	public void keepsTheId() throws Exception {
		Client client = new Client();
		UUID id = UUID.randomUUID();
		client.resource(INITIAL_URI).header(CONFIGURATION.getHeaderName(), id).post(ClientResponse.class);

		assertThat(MOCK_TEST.getRequestTrackerId(), equalTo(id.toString()));
	}
}