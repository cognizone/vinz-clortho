# Vinz Clortho
A lightweight replacement for Zuul when using spring boot 2.7+

## Get started
In your code just add the `@EnableVinzClorthoProxy` annotation on any `@Configuration` class.
Then setup the routing configuration like this:
```yaml
cognizone:
  vinz:
    httpClient:
      useSystemProperties: true/false # (default false) - since v2.0.1 
    routes:
      - name: firstRoute   # just a name, make it unique, nothing else special here 
        path: /proxy/route1/**  # the path on your server (if you use a servlet context path, this will be the part after the context path) 
        url: http://example.com/route1  # The destination url to proxy to
      - name: secondRoute
        path: /proxy/route2/**
        url: http://example.com/route2
        headers:
          response-set:
            - key: Access-Control-Allow-Headers
              value: "*"
              filter: "#{[request].getHeader('Origin') != null}"
          request-set:
            - key: X-Send-This-As-Header
              value: "someValue"
```
That's it, you're all set.

## What's supported
### Altering the body of the Request
Create a spring bean implementing `cogni.zone.vinzclortho.BodyEditor`.
If such a bean exists, it will be called with the original HttpRequest and content body as an InputStream, so you can change the content of the request body.
The new body has to be returned as a `org.apache.http.HttpEntity` object.

### Validate if a Request is allowed
Create a spring bean implementing `cogni.zone.vinzclortho.RequestValidator`.
If such a bean exists, it will be called with the original request information.
If you return a non-null value, that value with be used to send the response and no proxying will be done.

### HTTP methods
At the moment following HTTP methods are supported: GET, POST, PUT, DELETE

### Headers
#### Pass from original request
Follow headers will be passed from the original request to the destination:
- Accept
- Accept-Language
- Content-Type
- User-Agent

#### Add extra fixed headers

Using configuration `.headers.request-set` you can pass additional headers to the destination server. 
This can for example be used to set basic authentication headers.

#### Send back to caller
After the proxied request, following response headers will be sent back to the client:
- Content-Type

Using configuration `.headers.response-set` you can also set extra headers to send back to the caller.
The setting of these extra headers can be ignored based on filter which is evaluated as a SpEL expression.

#### Example info
In the example above `Access-Control-Allow-Headers: *` will be added to the response of secondRoute in case the filter matches.
The filter should be a spel expression that returns a boolean. 
In this example (`#{[request].getHeader('Origin') != null}`) the header will be set if the original request contains an `Origin` header.

The proxying will also send the header `X-Send-This-As-Header` with value `someValue` to the destination server.
