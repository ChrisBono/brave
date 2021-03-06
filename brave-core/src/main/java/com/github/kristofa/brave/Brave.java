package com.github.kristofa.brave;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Builds brave api objects.
 */
public class Brave {

    private final ServerTracer serverTracer;
    private final ClientTracer clientTracer;
    private final ServerRequestInterceptor serverRequestInterceptor;
    private final ServerResponseInterceptor serverResponseInterceptor;
    private final ClientRequestInterceptor clientRequestInterceptor;
    private final ClientResponseInterceptor clientResponseInterceptor;
    private final AnnotationSubmitter serverSpanAnnotationSubmitter;
    private final ServerSpanThreadBinder serverSpanThreadBinder;
    private final ClientSpanThreadBinder clientSpanThreadBinder;

    /**
     * Builds Brave api objects with following defaults if not overridden:
     *
     * <ul>
     *     <li>ThreadLocalServerAndClientSpanState which binds trace/span state to current thread.</li>
     *     <li>FixedSampleRateTraceFilter which traces every request.</li>
     *     <li>LoggingSpanCollector</li>
     * </ul>
     *
     */
    public static class Builder {

        private List<TraceFilter> traceFilters = new ArrayList<>();
        private SpanCollector spanCollector = new LoggingSpanCollector();
        private ServerAndClientSpanState state;
        private Random random = new Random();

        /**
         * Builder which initializes with serviceName = "unknown"
         */
        public Builder() {
            this("unknown");
        }

        /**
         * Builder.
         * @param serviceName Name of service. Is only relevant when we do server side tracing.
         */
        public Builder(String serviceName) {
            try {
                InetAddress a = InetAddressUtilities.getLocalHostLANAddress();
                state = new ThreadLocalServerAndClientSpanState(a, 0, serviceName);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to get Inet address", e);
            }
            traceFilters.add(new FixedSampleRateTraceFilter(1));
        }

        /**
         * Initialize trace filters. If not specified a default filter will be configured which traces every request.
         *
         * @param filters trace filters.
         */
        public Builder traceFilters(List<TraceFilter> filters) {
            traceFilters.clear();
            traceFilters.addAll(filters);
            return this;
        }

        /**
         * Allows you to use custom trace state object.
         *
         * @param state
         */
        public Builder traceState(ServerAndClientSpanState state) {
            this.state = state;
            return this;
        }

        /**
         *
         * @param spanCollector
         */
        public Builder spanCollector(SpanCollector spanCollector) {
            this.spanCollector = spanCollector;
            return this;
        }

        public Brave build() {
            return new Brave(this);
        }

    }

    /**
     * Client Tracer.
     * <p>
     * It is advised that you use ClientRequestInterceptor and ClientResponseInterceptor instead.
     * Those api's build upon ClientTracer and have a higher level api.
     * </p>
     *
     * @return ClientTracer implementation.
     */
    public ClientTracer clientTracer() {
        return clientTracer;
    }

    /**
     * Server Tracer.
     * <p>
     * It is advised that you use ServerRequestInterceptor and ServerResponseInterceptor instead.
     * Those api's build upon ServerTracer and have a higher level api.
     * </p>
     *
     * @return ClientTracer implementation.
     */
    public ServerTracer serverTracer() {
        return serverTracer;
    }

    public ClientRequestInterceptor clientRequestInterceptor() {
        return clientRequestInterceptor;
    }

    public ClientResponseInterceptor clientResponseInterceptor() {
        return clientResponseInterceptor;
    }

    public ServerRequestInterceptor serverRequestInterceptor() {
        return serverRequestInterceptor;
    }

    public ServerResponseInterceptor serverResponseInterceptor() {
        return serverResponseInterceptor;
    }

    /**
     * Helper object that can be used to propogate server trace state. Typically over different threads.
     *
     * @see ServerSpanThreadBinder
     * @return {@link ServerSpanThreadBinder}.
     */
    public ServerSpanThreadBinder serverSpanThreadBinder() {
        return serverSpanThreadBinder;
    }

    /**
     * Helper object that can be used to propagate client trace state. Typically over different threads.
     *
     * @see ClientSpanThreadBinder
     * @return {@link ClientSpanThreadBinder}.
     */
    public ClientSpanThreadBinder clientSpanThreadBinder() {
        return clientSpanThreadBinder;
    }

    /**
     * Can be used to submit application specific annotations to the current server span.
     *
     * @return Server span {@link AnnotationSubmitter}.
     */
    public AnnotationSubmitter serverSpanAnnotationSubmitter() {
        return serverSpanAnnotationSubmitter;
    }

    private Brave(Builder builder) {
        serverTracer = ServerTracer.builder()
                .randomGenerator(builder.random)
                .spanCollector(builder.spanCollector)
                .state(builder.state)
                .traceFilters(builder.traceFilters).build();

        clientTracer = ClientTracer.builder()
                .randomGenerator(builder.random)
                .spanCollector(builder.spanCollector)
                .state(builder.state)
                .traceFilters(builder.traceFilters).build();

        serverRequestInterceptor = new ServerRequestInterceptor(serverTracer);
        serverResponseInterceptor = new ServerResponseInterceptor(serverTracer);
        clientRequestInterceptor = new ClientRequestInterceptor(clientTracer);
        clientResponseInterceptor = new ClientResponseInterceptor(clientTracer);
        serverSpanAnnotationSubmitter = AnnotationSubmitter.create(SpanAndEndpoint.ServerSpanAndEndpoint.create(builder.state));
        serverSpanThreadBinder = new ServerSpanThreadBinder(builder.state);
        clientSpanThreadBinder = new ClientSpanThreadBinder(builder.state);
    }
}
