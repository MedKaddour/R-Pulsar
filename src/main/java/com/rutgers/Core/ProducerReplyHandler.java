/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Core;

import com.rutgers.Core.Message.ARMessage;
import static com.rutgers.Core.Message.ARMessage.Action.HELLO;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

/**
* Simple class implementation for pushing the messages to the queue.
* 
* @author  Eduard Giber Renart
* @version 1.0
*/
public class ProducerReplyHandler implements ObjectDataReply{
    
    private final BlockingQueue<Pair<PeerAddress, ARMessage>> queue;
    private final int type;
    
    public ProducerReplyHandler(BlockingQueue<Pair<PeerAddress, ARMessage>>  queue, int type) throws  IOException {
        this.queue = queue;
        this.type = type;
    }

    @Override
    /**
     * Implements a TomP2P method in order to get the messages and bring it to the RP space.
     */
    public Object reply(PeerAddress pa, Object o) throws IOException {
        ARMessage msg = ARMessage.class.cast(o);
        Pair p = new Pair<>(pa, msg);
        queue.add(p);   
        
        if(msg.getAction() == HELLO)
            return type;
        else
            return 0;
    }
}
