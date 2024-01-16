# Vinz Clortho
A lightweight replacement for Zuul when using spring boot 2.7+

## Get started
In your code just add the `@EnableVinzClorthoProxy` annotation on any `@Configuration` class.
Then setup the routing configuration like this:
```yaml
cognizone:
  vinz:
    routes:
      - name: firstRoute   # just a name, make it unique, nothing else special here 
        path: /proxy/route1/**  # the path on your server (if you use a servlet context path, this will be the part after the context path) 
        url: http://example.com/route1  # The destination url to proxy to
      - name: secondRoute
        path: /proxy/route2/**
        url: http://example.com/route2
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
Follow headers will be passed from the original request:
- Accept
- Accept-Language
- Content-Type
- User-Agent

After the proxied request, following response headers will be send back to the client:
- Content-Type

 
