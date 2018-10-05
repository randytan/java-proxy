package me.randytan.proxy;

import me.randytan.proxy.helper.Proxy;
import me.randytan.proxy.helper.Utilities;
import me.randytan.proxy.model.SystemProp;

public class Main {

    public static void main(String[] args) {

        Utilities.loadConfiguration();
        Proxy proxy = new Proxy(SystemProp.getBindPort());
        proxy.listen();
    }

}
