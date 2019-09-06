/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Core;

import org.apache.storm.thrift.TException;

/**
* This class is the interface for the message listener.
*
* @author  Eduard Giber Renart
* @version 1.0
*/
public interface Listener {
    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param   o     the observable object.
     * @param   arg   an argument passed to the <code>notifyObservers</code>
     *                 method.
     * @throws TException 
     */
    void replay(MessageListener o, Message.ARMessage arg) throws TException;
}
