/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;

import com.google.common.io.Resources;
import com.rutgers.Core.Message;
import com.rutgers.Core.PulsarProducer;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is and example of the use of the R-Pulsar API.
 * This example shows how to build a R-Pulsar DHT Publisher.
 * @author eduard
 *
 */
public class DHTPublisher {
    
    static PulsarProducer producer = null;
    
    public static void main(String[] args) throws UnknownHostException, ClassNotFoundException {
        try {
            // TODO code application logic here
            InputStream props = Resources.getResource(args[0]).openStream();
            Properties properties = new Properties();
            properties.load(props);
            
            //Create an R-Pulsar consumer
            producer = new PulsarProducer(properties);
            //Init the consumer
            producer.init();

            int size = Integer.parseInt(args[0]);
            
            List<String> payloadList = new ArrayList<>();
            for(int i = 0; i < size; i++) {
                String num = String.valueOf(new Random().nextInt(15000));
                while (payloadList.contains(num)) {
                    num = String.valueOf(new Random().nextInt(15000));
                }
                payloadList.add(num);
            }

            //Create a header with the profile
            Message.ARMessage.Header.Profile profile = Message.ARMessage.Header.Profile.newBuilder().addSingle("temperature").addSingle("fahrenheit").build();
            Message.ARMessage.Header header = Message.ARMessage.Header.newBuilder().setLatitude(0.00).setLongitude(0.00).setType(Message.ARMessage.RPType.AR_PRODUCER).setProfile(profile).setPeerId(producer.getPeerID()).build();
            //Telling the RP to store the message in the DHT
            Message.ARMessage msg = Message.ARMessage.newBuilder().setHeader(header).setAction(Message.ARMessage.Action.STORE_DATA).addAllPayload(payloadList).build();
           
            System.out.println("DHT Insert start: " + System.currentTimeMillis());
            //Sending the message to the RP
            producer.post(msg, profile);
            
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException | InterruptedException ex) {
            Logger.getLogger(DHTPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
