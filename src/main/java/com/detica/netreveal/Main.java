package me.randytan.proxy;

import me.randytan.proxy.helper.Proxy;

public class Main {

    public static void main(String[] args) {
        Proxy proxy = new Proxy(11889);
        proxy.listen();
    }

}
