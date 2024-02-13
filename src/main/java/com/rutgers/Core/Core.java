/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Core;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import static com.rutgers.Core.Globals.*;
import com.rutgers.Core.Message.ARMessage;
import com.rutgers.Core.Message.ARMessage.Header;
import com.rutgers.Core.Message.ARMessage.Header.Profile;
import com.rutgers.DB.RocksDBMS;
import com.rutgers.QuadTree.PointQuadTree;
import com.rutgers.Telemetry.TelemetryConfiguration;

import java.util.ArrayList;
//import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.util.Base64;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;

/**
 *
 * @author eduard
 */
public class Core {

    
	   /**
	   * This is the main method which instantiates the R-Pulsar node
	   * @param args The arguments need to follow the CLI implementation.
	   * @return Nothing.
	   */
    public static void main(String[] args) throws InterruptedException, IOException, InvalidKeySpecException, ClassNotFoundException {
    	 System.out.println("╔════════════════════════════════════════════════════════════════╗");
         System.out.println("║ ██████═╗      ██████╗ ██    ██ ███╗   ███████╗  █████╗██████═╗ ║");
         System.out.println("║ ██╔═██═╝      ██╔══██╗██ ╔══██ ██╗    ██╔════╝██╔══██╗██╔═██═╝ ║");
         System.out.println("║ ███████╗█████ ██████╔╝██═╝  ██ ██     ███████╗███████║███████╗ ║");
         System.out.println("║ ██═══██║      ██╔═══╝ ██╔═══██╔██║    ╚════██║██╔══██║██═══██║ ║");
         System.out.println("║ ██   ██║      ██║     ████████║██████║███████║██║  ██║██   ██║ ║");
         System.out.println("╚═════════════════════════════1.0════════════════════════════════╝");
    	
        BlockingQueue<Pair<PeerAddress, ARMessage>> messageQueue;
        BlockingQueue<ARMessage> userQueue;
        BlockingQueue<ARMessage> pollQueue;
        ExecutorService executorService;
        Thread pingThread;
        Manager manager;
        HeartBeat ping;
        RP rp;
        LocationKeyManager lkManager;
        QueueManager qManager;
        PointQuadTree<PeerAddress> qTree;
        TelemetryConfiguration.SERVICE_NAME="Master rendez-vous Service";
        
        List<ConsumerReplyHandler> replyHandlers = new ArrayList<>(_THREAD_POOL_);
        
        
        //Tracer tracer;
        //tracer = tracerProvider.get("com.rutgers.Core");
        //Span span = tracer.spanBuilder("Start  Rendez Vous Node").startSpan();
        try {
        	//span.addEvent("Event 0:start");
            Cli c = new Cli(args);
            c.parse();
            rp = new RP(c.port, c.dir,c.trace);
            executorService = Executors.newFixedThreadPool(_THREAD_POOL_); 
            lkManager = new LocationKeyManager();
            qManager = new QueueManager();
            
            manager = Manager.getInstance();
            manager.setLocationKeyManager(lkManager);
            manager.setRpOne(rp);
            manager.setRocksDBMS(new RocksDBMS());
            manager.setQueueManager(qManager);
            manager.setLatitude(Double.parseDouble(c.gps[0]));
            manager.setLongitude(Double.parseDouble(c.gps[1]));
            
            /**
             * Checks if the boot flag in the CLI is set to true
             * If set to false will start and R-Pulsar Master
             * If set the true will start and R-Pulsar Slave
             */
            if(!c.boot[0].equals("")) {
            	/**
            	 * Start RP slave.
            	 */
                manager.setMaster(false);
                
                if(c.repli != 0)
                    rp.startDHTBootstrap(c.boot[0], c.boot[1], c.repli);
                else 
                    rp.startDHTBootstrap(c.boot[0], c.boot[1], _DEF_REPLICATION_);
                //span.addEvent("Event 1:finished bootstrapping");
                messageQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                userQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                pollQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                
                rp.setupReplyHandler(messageQueue, 0);
                
                for(int i = 0; i < _THREAD_POOL_; i++) {
                	replyHandlers.add(new ConsumerReplyHandler(messageQueue, userQueue, pollQueue));
                    executorService.execute(replyHandlers.get(i));
                }

                String publicString = Base64.getEncoder().encodeToString(rp.getUserProfile().getPublicKey().getEncoded());       
                rp.putDHT(rp.getId(), new Number160("RP".getBytes()), publicString);
                
                /**
                 * Send AR_HELLO to the Master RP so he can add us to the list.
                 */
                Profile.Builder p = ARMessage.Header.Profile.newBuilder();
                Header h = ARMessage.Header.newBuilder().setLatitude(manager.getLatitude()).setLongitude(manager.getLongitude()).setType(ARMessage.RPType.AR_RP).setProfile(p).addHID(rp.getId().toString()).build();
                ARMessage msg = ARMessage.newBuilder().setHeader(h).setAction(ARMessage.Action.HELLO).build();
                
                Collection<PeerAddress> addressList = rp.getAllKnownPeers();
                for (PeerAddress element : addressList) {
                    int type = (int)rp.sendDirectMessageBlocking(element, msg);
                    if(type == 1)
                        lkManager.insertKey(element.peerId(), element);
                }
                /**
                 * Start and R-Pulsar master.
                 */
            }else {
            	/**
            	 * Check if user specified a replication factor.
            	 */
                if(c.repli != 0)
                    rp.startDHTMaster(c.repli);
                else 
                    rp.startDHTMaster(_DEF_REPLICATION_);

                //span.addEvent("Event 1:finished bootstrapping");
                //span.end();
                manager.setMaster(true);
                
                if(manager.getRpTwo() != null) {
                    ping = new HeartBeat(false);
                    pingThread = new Thread(ping);
                    pingThread.start();
                }else {
                    ping = new HeartBeat(true);
                    pingThread = new Thread(ping);
                    pingThread.start();
                }
                /**
                 * Init the quadtree and add yourself into it.
                 */
                if("".equals(c.area[0])) {
                    qTree = new PointQuadTree<PeerAddress>(_DEF_NORTH_, _DEF_SOUTH_, _DEF_EAST_, _DEF_WEST_, 4, 4);
                } else {
                    qTree = new PointQuadTree<PeerAddress>(Double.parseDouble(c.area[0]), Double.parseDouble(c.area[1]), Double.parseDouble(c.area[2]), Double.parseDouble(c.area[3]), 4, 4);
                }
               
                qTree.insert(manager.getLatitude(), manager.getLongitude(), rp.getPeerAddress(), "");
                manager.setqTree(qTree);
                String publicString = Base64.getEncoder().encodeToString(rp.getUserProfile().getPublicKey().getEncoded());           
                //rp.putDHT(rp.getId(), new Number160("RP".getBytes()), publicString);
                
                messageQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                userQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                pollQueue = new ArrayBlockingQueue<>(_MAX_QUEUE_SIZE_);
                
                rp.setupReplyHandler(messageQueue, 0);
                Span listnerspan=null;
            	if (rp.tracer != null) {
            		listnerspan = rp.tracer.spanBuilder("Starting "+ _THREAD_POOL_+" Consumer Handlers").setParent(Context.current().with(rp.RootSpan)).startSpan();
            		// adding event to the span 
            		listnerspan.addEvent("Event 0:starting");
            	}
            	 for(int i = 0; i < _THREAD_POOL_; i++) {
                 	replyHandlers.add(new ConsumerReplyHandler(messageQueue, userQueue, pollQueue));
                     executorService.execute(replyHandlers.get(i));
                 }
                if (rp.tracer  != null) {
                	// adding event to the span 
                	listnerspan.addEvent("Event X:finished");
                	listnerspan.end();
                }
				
				/*
				 * try { Thread.sleep(50000); for(int i = 0; i < _THREAD_POOL_; i++) {
				 * replyHandlers.get(i).setRunning(false); }
				 * 
				 * 
				 * 
				 * 
				 * System.out.println("Stopping RP"); Thread.sleep(10000); rp.stop();
				 * System.exit(0);
				 * 
				 * } catch (InterruptedException e) { e.printStackTrace(); }
				 */
				 
            }
        } catch (IOException | InterruptedException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
