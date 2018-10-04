package com.detica.netreveal;

import com.detica.netreveal.helper.Proxy;

public class Main {

    public static void main(String[] args) {
        Proxy proxy = new Proxy(11889);
        proxy.listen();
    }

}
