# Java Proxy Traffic Redirector

This is a custom java solution for traffic redirector if application didn't honour the Environment Variables (HTTP_PROXY and NO_PROXY) configuration.

## Getting Started

Checkout the code, run the Main class with parameters.

## Usage

Add parameters or pass parameters to supply proxy host and port forward and list of no-proxy domain/IP address.

```
-Dhttp.proxyHost=proxy1.localhost
-Dhttp.proxyPort=8080
-Dhttp.nonProxyHosts=localhost|127.0.*|10.*|\\*.technology
-Dhttps.proxyHost=proxy1.localhost
-Dhttps.proxyPort=8080
```

## Versioning

Initial Beta (v1)

## Authors

* Randy Tanudjaja 

## License

Apache 2.0 License.

