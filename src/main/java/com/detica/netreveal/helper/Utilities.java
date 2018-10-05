/*
 * Software Copyright BAE Systems plc 2018. All Rights Reserved.
 * BAE SYSTEMS, DETICA and NETREVEAL are trademarks of BAE Systems
 * plc and may be registered in certain jurisdictions.
 */

package com.detica.netreveal.helper;

public class Utilities {

    protected static void unsetProxy(){
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
    }

}
