/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;

import com.rutgers.Core.Listener;
import com.rutgers.Core.Message;
import com.rutgers.Core.Message.ARMessage;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

import com.rutgers.Core.MessageListener;
import com.rutgers.Core.PulsarProducer;

import static com.rutgers.Core.Globals._THREAD_POOL_;
import com.rutgers.Telemetry.GlobalTracing;
import com.rutgers.Telemetry.TelemetryConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is and example of the use of the R-Pulsar API.
 * This example shows how to build a R-Pulsar Publisher.
 * @param keys
 * @return
 */
public class HelloWorldPublisher { 
    
    static int numRecords = 10000;
    static int recordSize = 200;
    static int iterations = 20;
    static boolean running = false;
    static Thread thread = null;
    static PulsarProducer producer = null;
    
    
    public static class Push implements Runnable {
        Message.ARMessage msg = null;
        ARMessage.Header.Profile profile = null;
        ARMessage.Header header = null;
        String[] sentences = null;
        
        Push(Message.ARMessage msg) {
            this.msg = msg;
            sentences = new String[]{ "the cow jumped over the moon", "an apple a day keeps the doctor away",
                    "four score and seven years ago", "snow white and the seven dwarfs", "i am at two with nature" };
        }
        
        @Override
        public void run() {
        	// Creation of a record
            Random rand = new Random(); 
            //Using some compression techniques to reduce network overhead
        	Message.ARMessage push_msg = Message.ARMessage.newBuilder().setAction(Message.ARMessage.Action.STORE_QUEUE).setTopic(msg.getTopic()).addPayload("Hello World!!").build();
        	/************************************************************/
        	 Span  span = GlobalTracing.RetrieveContextAndStartSpan(msg, "Producer");
        
            /**************************************************************/
            for(int i = 0; i < iterations; i ++) {
                try {
                 	
		            
                	System.out.println("Sending: Hello World!!");
                	
                    producer.stream(push_msg, msg.getHeader().getPeerId());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnknownHostException | InterruptedException ex) {
                    Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            span.end();
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException, ClassNotFoundException {
        try {
        	
        	if(args.length == 0) {
    	        System.out.println("Need to specify the producer property file!!");
    	        System.exit(0);
        	}
        	
            // TODO code application logic here           
            InputStream props = new FileInputStream(args[0]);//Resources.getResource("producer.prop").openStream();
            Properties properties = new Properties();
            properties.load(props);

            TelemetryConfiguration.SERVICE_NAME="Producer Service";
            producer = new PulsarProducer(properties);
            producer.init();
            
            producer.replayListener(new Listener(){
                @Override
                public void replay(MessageListener ml, Message.ARMessage o) {
                    switch(o.getAction()) {
                    	//Start streaming the sensor data
                        case NOTIFY_START:
                            running = true;
                            
                            //Start a new thread with Push class
                            thread = new Thread(new Push(o));
                            thread.start();
                            
                            break;
                        case NOTIFY_STOP:
                            try {
                                running = false;
                                thread.join();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                    }
                }
            });
			/*
			 * Span span=null;
			 * 
			 * if (producer.getRp().tracer != null) { span =
			 * producer.getRp().tracer.spanBuilder("testing distributed tracing").startSpan(
			 * ); // adding event to the span span.addEvent("Event 0:starting"); } Context
			 * currentContext = Context.current().with(span); // Extract the SpanContext
			 * from the current context SpanContext spanContext =
			 * Span.fromContext(currentContext).getSpanContext();
			 * 
			 * // Retrieve the trace ID and span ID String traceId =
			 * spanContext.getTraceId(); String spanId = spanContext.getSpanId();
			 * 
			 * System.out.print("prod: trace_id="+traceId+" span_id="+spanId); //Create
			 * sensor profile
			 */            ARMessage.Header.Profile profile = ARMessage.Header.Profile.newBuilder()
            		.addSingle("temperature").addSingle("fahrenheit").build();
            
            ARMessage.Header header = ARMessage.Header.newBuilder().setLatitude(0.10).setLongitude(0.50)
            		.setType(ARMessage.RPType.AR_PRODUCER).setProfile(profile).setPeerId(producer.getPeerID())
            		.build();
            ARMessage msg = ARMessage.newBuilder().setHeader(header).setAction(ARMessage.Action.NOTIFY_INTEREST)
            		.build();
           
            producer.post(msg, profile);
          
            try {
            	Thread.sleep(60000);
            	System.out.println("Stopping producer");
            	producer.shutdown();
         
				  System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
