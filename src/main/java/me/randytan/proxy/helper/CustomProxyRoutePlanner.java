package me.randytan.proxy.helper;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomProxyRoutePlanner extends DefaultRoutePlanner  {

    static final Logger LOG = LogManager.getLogger(RequestHandler.class);

    public CustomProxyRoutePlanner(SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
    }

    private HttpHost getProxy(String scheme) {

        String proto = scheme;

        String proxyHostKey = proto + ".proxyHost";
        String proxyPortKey = proto + ".proxyPort";
        String protoProxyHost = System.getProperty(proxyHostKey);
        if (protoProxyHost == null) {
            protoProxyHost = System.getenv(proxyHostKey);
        }
        if (protoProxyHost == null) {
            return null;
        }
        String proxyPortStr = System.getProperty(proxyPortKey);
        if (proxyPortStr == null) {
            proxyPortStr = System.getenv(proxyPortKey);
        }
        if (proxyPortStr == null) {
            return null;
        }
        int protoProxyPort = -1;
        if (proxyPortStr != null) {
            try {
                protoProxyPort = Integer.valueOf(proxyPortStr);
            } catch (NumberFormatException nfe) {
                LOG.info("invalid {} : {} proxy will be ignored", proxyPortKey, proxyPortStr);
                return null;
            }
        }
        if (protoProxyPort < 1) {
            return null;
        }
        LOG.debug("set {} proxy '{}:{}'", proto, protoProxyHost, protoProxyPort);
        return new HttpHost(protoProxyHost, protoProxyPort, "http");
    }

    private String[] getNonProxyHosts(String uriScheme) {
        String nonproxyHostKey = uriScheme + ".nonProxyHosts";
        String nonproxyHost = System.getProperty(nonproxyHostKey);
        if (nonproxyHost == null) {
            nonproxyHost = System.getenv(nonproxyHostKey);
        }
        if (nonproxyHost == null) {
            return null;
        }
        return nonproxyHost.split("\\|");
    }

    private boolean doesTargetMatchNonProxy(HttpHost target) {
        String uriHost  = target.getHostName();
        String uriScheme = target.getSchemeName();
        String[] nonProxyHosts = getNonProxyHosts(uriScheme);
        int nphLength = nonProxyHosts != null ? nonProxyHosts.length : 0;
        if (nonProxyHosts == null || nphLength < 1) {
            LOG.debug("scheme:'{}', host:'{}' : DEFAULT (0 non proxy host)", uriScheme, uriHost);
            return false;
        }
        for (String nonProxyHost : nonProxyHosts) {
            if (uriHost.matches(nonProxyHost)) {
                LOG.debug("scheme:'{}', host:'{}' matches nonProxyHost '{}' : NO PROXY", uriScheme, uriHost, nonProxyHost);
                return true;
            }
        }
        LOG.debug("scheme:'{}', host:'{}' : DEFAULT  (no match of {} non proxy host)", uriScheme, uriHost, nphLength);
        return false;
    }

    @Override
    protected HttpHost determineProxy(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws HttpException {

        if (doesTargetMatchNonProxy(target)) {
            LOG.debug("Target Match Proxy.");
            return null;
        } else {
            LOG.debug("Target Doesn't Match Non Proxy.");
        }

        return getProxy(target.getSchemeName());
    }
}