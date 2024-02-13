package com.rutgers.Telemetry;

import static com.rutgers.Core.Globals._THREAD_POOL_;

import java.util.TreeMap;

import com.rutgers.Core.Message.ARMessage;
import com.rutgers.Core.RP;

import io.opentelemetry.context.Context;
import net.tomp2p.peers.Number160;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

public class GlobalTracing {
    private final Span span;
    private final ARMessage arMessage;
    // A TreeMap to manage spans based on Number160 keys
    static private TreeMap<Number160, Span> spansManaager = new TreeMap<Number160, Span>();

    // Constructor
    public GlobalTracing(Span span, ARMessage arMessage) {
        this.span = span;
        this.arMessage = arMessage;
    }
    
    static public void addSpan(Number160 Hid, Span span) {
    	
    	synchronized (spansManaager) {
    		spansManaager.put(Hid, span);
		}
    	
    	
    	
    }
    
static public Span getSpan(Number160 Hid) {
    	Span span=null;
    	synchronized (spansManaager) {
    		span=spansManaager.get(Hid);
		}
    	
    	return span;
    	
    }
    
    
    
    // Getter for Span
    public Span getSpan() {
        return span;
    }

    // Check if ARMessage has trace information
    public static boolean hasTraces(ARMessage armsg) {
        // Check if both traceId and spanId are not null
        return (armsg.getHeader().getSpanId() != null) && (armsg.getHeader().getTraceId() != null);
    }

    // Getter for ARMessage
    public ARMessage getARMessage() {
        return arMessage;
    }

    // Method to inject context into ARMessage
    public static ARMessage InjectContext(ARMessage armsg, Span span, RP peer) {
        if (peer.tracer != null) {
            // Create a new context with the provided span
            Context currentContext = Context.current().with(span);

            // Extract the SpanContext from the current context
            SpanContext spanContext = Span.fromContext(currentContext).getSpanContext();

            // Retrieve the trace ID and span ID from the SpanContext
            String traceId = spanContext.getTraceId();
            String spanId = spanContext.getSpanId();

            // Update the ARMessage header with the trace ID and span ID
            ARMessage.Header header = ARMessage.Header.newBuilder(armsg.getHeader()).setTraceId(traceId)
                    .setSpanId(spanId).build();

            // Return a new ARMessage with the updated header
            armsg = ARMessage.newBuilder(armsg).setHeader(header).build();
        }
        return armsg;
    }

    // Method to migrate context from source ARMessage to destination ARMessage
    public static ARMessage MigrateContext(ARMessage src_armsg, ARMessage dest_armsg) {
        String traceId = src_armsg.getHeader().getTraceId();
        String spanId = src_armsg.getHeader().getSpanId();

        // Update the ARMessage header with the trace ID and span ID
        ARMessage.Header header = ARMessage.Header.newBuilder(dest_armsg.getHeader()).setTraceId(traceId)
                .setSpanId(spanId).build();

        // Return a new ARMessage with the updated header
        dest_armsg = ARMessage.newBuilder(dest_armsg).setHeader(header).build();
        return dest_armsg;
    }

    // Method to start a new Span and inject context into ARMessage
    public static GlobalTracing StartSpanAndInjectContext(ARMessage received_armsg, ARMessage armsg, String spanName,
            RP peer) {
        Span span = null;

        if (peer.tracer != null) {
            // Start a new Span using the provided tracer
            span = peer.tracer.spanBuilder(spanName).startSpan();

            // Create a new context with the provided span
            Context currentContext = Context.current().with(span);

            // Extract the SpanContext from the current context
            SpanContext spanContext = Span.fromContext(currentContext).getSpanContext();

            // Retrieve the trace ID and span ID from the SpanContext
            String traceId = spanContext.getTraceId();
            String spanId = spanContext.getSpanId();

            // Update the ARMessage header with the trace ID and span ID
            ARMessage.Header header = ARMessage.Header.newBuilder(armsg.getHeader()).setTraceId(traceId)
                    .setSpanId(spanId).build();

            // Return a new ARMessage with the updated header
            armsg = ARMessage.newBuilder(armsg).setHeader(header).build();
        }
        return new GlobalTracing(span, armsg);
    }

    // Method to retrieve context from ARMessage and start a new Span
    public static Span RetrieveContextAndStartSpan(ARMessage armsg, String spanName) {
        // Extract trace_id and span_id from the ARMessage header
        String traceId = armsg.getHeader().getTraceId();
        String spanId = armsg.getHeader().getSpanId();

        // Check if both traceId and spanId are not null
        if (traceId != null && spanId != null) {
            // Create a new SpanContext with the extracted traceId and spanId
            SpanContext parentContext = SpanContext.createFromRemoteParent(traceId, spanId, TraceFlags.getSampled(),
                    TraceState.getDefault());

            // Use the new SpanContext to create a new Span
            return GlobalOpenTelemetry.getTracer("").spanBuilder(spanName)
                    .setParent(Context.current().with(Span.wrap(parentContext))).startSpan();
        }
        return null;
    }
    public static Span CreateLocalSpan(RP rp,String SpanName) {
    	Span span=null;
    	if (rp.tracer != null) {
    		if(rp.RootSpan!=null) {
    			span = rp.tracer.spanBuilder(SpanName).setParent(Context.current().with(rp.RootSpan)).startSpan();
    		}
    	}
    	return span;
    	
    }
}
