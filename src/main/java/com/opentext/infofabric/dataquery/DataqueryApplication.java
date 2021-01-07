/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.opentext.infofabric.dataquery.cache.ResponseCacheFilter;
import com.opentext.infofabric.dataquery.endpoints.DataqueryHealthCheckServlet;
import com.opentext.infofabric.dataquery.endpoints.GraphQLServlet;
import com.opentext.infofabric.dataquery.endpoints.NamedQueryServlet;
import com.opentext.infofabric.dataquery.endpoints.NamedQueryTaskResource;
import com.opentext.infofabric.dataquery.endpoints.RawQueryServlet;
import com.opentext.infofabric.dataquery.endpoints.VersionResource;
import com.opentext.infofabric.dataquery.guice.DataqueryModule;
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.health.DataCastHealthCheck;
import com.opentext.infofabric.dataquery.health.DatamodelHealthCheck;
import com.opentext.infofabric.dataquery.health.DataqueryHealthCheck;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionFactory;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionProvider;
import com.opentext.infofabric.dataquery.util.AppStateService;
import com.opentext.infofabric.dataquery.util.RegistrarInitializer;
import com.opentext.infofabric.common.crypto.IFabricCryptoService;
import com.opentext.infofabric.common.dropwizard.security.IFabricAuthenticator;
import com.opentext.infofabric.common.dropwizard.security.IFabricAuthorizer;
import com.opentext.infofabric.common.dropwizard.security.IFabricJwtFilter;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.stream.Stream;

/**
 * Dropwizard Application
 */
public class DataqueryApplication extends Application<DataqueryConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DataqueryApplication.class);

    public static final String APP_NAME = "infofabric-dataquery";
    private static final String DATAQUERY = "dataquery";
    private static final String DATAQUERY_ENDPOINTS = "com.liaison.dataquery.endpoints";
    private static final String HEALTH_CHECK_DATAMODEL = "datamodel";
    private static final String HEALTH_CHECK_DATACAST = "datacast";

    private static final String GRAPHQL_SERVLET = "graphQLServlet";
    private static final String RAWQUERY_SERVLET = "rawQueryServlet";
    private static final String NAMEDQUERY_SERVLET = "namedQueryServlet";

    public static void main(final String[] args) throws Exception {
        new DataqueryApplication().run(args);
    }

    @Inject
    private IFabricCryptoService cryptoService;

    @Override
    public String getName() {
        return APP_NAME;
    }

    @Override
    public void initialize(final Bootstrap<DataqueryConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/swagger", "/api", "index.html", "swagger"));
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final DataqueryConfiguration dataqueryConfiguration, final Environment environment)
            throws Exception {
        setSystemProperties();
        MetricReporter.initReporter(dataqueryConfiguration, environment);

        GuiceInjector.init(getGuiceModule(dataqueryConfiguration, environment));
        Injector injector = GuiceInjector.getInjector();
        NamedQueryConfiguration namedQueryConfiguration = NamedQueryConfiguration.getInstance();
        namedQueryConfiguration.setAppState(dataqueryConfiguration.getAppState());
        namedQueryConfiguration.setEnvironment(dataqueryConfiguration.getEnvironment());
        namedQueryConfiguration.setDataStreamPath(dataqueryConfiguration.getDatastream().getStreamPath());
//        namedQueryConfiguration.setStreamImplementation(dataqueryConfiguration.getStreamImplementation());

        NamedQueryConnectionFactory.setNamedQueryConnectionProvider(injector.getInstance(NamedQueryConnectionProvider.class));

        AppStateService.callConsumerRequest();
        AppStateService.buildDMPermissionCache();
//        environment.jersey().register(injector.getInstance(AuthenticateResource.class));
//        environment.jersey().register(injector.getInstance(AdminLoginService.class));

//        // Authentication filters
//        RestAuthFilter restAuthFilter = new RestAuthFilter.Builder<UserInfo>()
//                .setAuthenticator(injector.getInstance(RestAuthenticator.class))
//                .setAuthorizer(injector.getInstance(RestAuthorizer.class)).buildAuthFilter();
//        restAuthFilter.setEnableSSO(dataqueryConfiguration.isEnableSSO());
//        environment.jersey().register(new AuthDynamicFeature(restAuthFilter));
//        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(UserInfo.class));
//        environment.jersey().register(RolesAllowedDynamicFeature.class);

        // Health Check
        environment.healthChecks().register(APP_NAME, new DataqueryHealthCheck());
        environment.healthChecks().register(HEALTH_CHECK_DATAMODEL, injector.getInstance(DatamodelHealthCheck.class));
        environment.healthChecks().register(HEALTH_CHECK_DATACAST, injector.getInstance(DataCastHealthCheck.class));

        environment.jersey().register(VersionResource.class);

        //Add the security
        IFabricJwtFilter jwtAuthFilter = new IFabricJwtFilter(new IFabricAuthenticator(), new IFabricAuthorizer());
        environment.jersey().register(new AuthDynamicFeature(jwtAuthFilter));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(com.opentext.infofabric.common.dropwizard.security.UserInfo.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);

        injector.getInstance(RegistrarInitializer.class).initialize();

        // Admin context security handler
//        environment.getAdminContext().setSecurityHandler(injector.getInstance(AdminConstraintSecurityHandler.class));


        // Swagger
        environment.jersey().register(new ApiListingResource());
        environment.jersey().register(new SwaggerSerializers());
        BeanConfig config = new BeanConfig();
        config.setTitle(DATAQUERY);
        config.setResourcePackage(DATAQUERY_ENDPOINTS);
        config.setBasePath(getApplicationContextPath(dataqueryConfiguration));
        config.setScan(true);

        environment.jersey().register(NamedQueryTaskResource.class);
        environment.jersey().register(GraphQLServlet.class);
        environment.jersey().register(RawQueryServlet.class);
        environment.jersey().register(NamedQueryServlet.class);

        environment.servlets().addServlet("HealthCheckServlet", new DataqueryHealthCheckServlet(environment.healthChecks()))
                .addMapping("/" + DataqueryConstants.API_VERSION_V1 + "/" + DataqueryConstants.API_NAME + "/health");

        // Add Prometheus Metrics support
        CollectorRegistry collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(environment.metrics()));
        environment.servlets()
                .addServlet("prometheusMetrics", new MetricsServlet())
                .addMapping("/metrics");
        DefaultExports.initialize();

//        DataqueryUtils.addZooKeeperInfo(dataqueryConfiguration);
//        logger.info("Zooker quorum {}", dataqueryConfiguration.getLoadBalancerConfig().get("quorum"));
//        logger.info("Zooker Port {}", dataqueryConfiguration.getLoadBalancerConfig().get("clientPort"));

        // Add Query Servlet
//        environment.servlets().addServlet(RAWQUERY_SERVLET, injector.getInstance(RawQueryServlet.class))
//                .addMapping(DataqueryConstants.API_ROOT + "query/raw/*");

        // Add  GraphQLServlet
//        environment.servlets().addServlet(GRAPHQL_SERVLET, injector.getInstance(GraphQLServlet.class))
//                .addMapping(DataqueryConstants.API_ROOT + "query/graphql/*");

        // Add  NamedQueryServlet
//        environment.servlets().addServlet(NAMEDQUERY_SERVLET, injector.getInstance(NamedQueryServlet.class))
//                .addMapping(DataqueryConstants.API_ROOT + "query/named/*");

//        environment.servlets().addFilter("ServletFilter", ServletFilter.class)
//                .addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, RAWQUERY_SERVLET, GRAPHQL_SERVLET, NAMEDQUERY_SERVLET);

//        environment.servlets().addFilter("ResponseCacheFilter", ResponseCacheFilter.class)
//                .addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, GRAPHQL_SERVLET);

        configureHttpsConnector(dataqueryConfiguration);
    }

    protected AbstractModule getGuiceModule(DataqueryConfiguration dataqueryConfiguration, Environment environment) {
        return new DataqueryModule(dataqueryConfiguration, environment);
    }

    private String getApplicationContextPath(final DataqueryConfiguration config) {
        ServerFactory sf = config.getServerFactory();

        if (sf instanceof SimpleServerFactory) {
            return ((SimpleServerFactory) sf).getApplicationContextPath();
        } else {
            return ((DefaultServerFactory) sf).getApplicationContextPath();
        }
    }

    private void setSystemProperties() {
        System.setProperty("archaius.deployment.applicationId", APP_NAME);
    }

    private void configureHttpsConnector(final DataqueryConfiguration conf) {
//        if (conf.getSymmetricKeyPath() != null && !conf.getSymmetricKeyPath().isEmpty()) {
            DefaultServerFactory serverFactory = (DefaultServerFactory) conf.getServerFactory();
            Stream.concat(serverFactory.getApplicationConnectors().stream(),
                    serverFactory.getAdminConnectors().stream())
                    .filter(connector -> connector.getClass().equals(HttpsConnectorFactory.class))
                    .forEach(connector -> {
                        final HttpsConnectorFactory httpsConnectorFactory = (HttpsConnectorFactory) connector;
                        if (httpsConnectorFactory.getKeyStorePassword() != null) {
                            httpsConnectorFactory.setKeyStorePassword(cryptoService.getDecryptedData( httpsConnectorFactory.getKeyStorePassword()));
                        }
                    });
//        }
    }
}
