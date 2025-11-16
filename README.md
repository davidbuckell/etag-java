# entity-tags-demo
A prototype demonstrating the use of entity tags to perform cache busting when requested content has been modified.

Install, build & run the application.

`./mvnw spring-boot:run`

In a new terminal window make an initial CURL request to:

`curl -i http://localhost:8080/items/1 -w '\n'`.

This will return the full request body including an `ETag` header.
```
HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 16 Nov 2025 23:00:19 GMT
ETag: "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="
Transfer-Encoding: chunked
```

The following script will continuously ping the service every second passing the ETag argument within the `if-none-match` request header.

```
#!/bin/bash

ETAG_HEADER="$1"
URL="http://localhost:3000/items/1"

while true; do
  RESPONSE=$(curl -s -i -H "If-None-Match: \"$ETAG_HEADER\"" "$URL")

  STATUS=$(echo "$RESPONSE" | head -n1 | awk '{print $2}')
  RESPONSE_ETAG=$(echo "$RESPONSE" | grep -i '^ETag:' | awk '{print $2}' | tr -d '\r')

  # Only print ETag if non-empty
  if [ -n "$RESPONSE_ETAG" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS ETag: $RESPONSE_ETAG"
  else
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS"
  fi

  # Print body for 200 responses
  if [ "$STATUS" = "200" ]; then
    BODY=$(echo "$RESPONSE" | sed -n '/^\s*$/,$p' | tail -n +2)
    echo "$BODY"
  fi

  echo "----------------------------------------"
  sleep 1
done
```

Ensure the script is executable via:

`chmod +x client.sh`

Then execute the script passing in the correct ETag value
```
./client.sh "NDMgbWlucyBwYXN0IHRoZSBob3VyLTEtcGtvYmI3"
```

Whilst within the same minute, the response status code will be 304 with no response body :
```
2025-11-16 23:00:50 Status: 304

----------------------------------------
```

As soon as the current minute has elapsed the response will revert to the original 200 with response body until the new ETag value is passed within the request header.

Example output:

`curl -i http://localhost:8080/items/1 -w '\n'`

```
HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 16 Nov 2025 23:00:19 GMT
ETag: "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="
Transfer-Encoding: chunked```

The following script will continuously ping the service every second passing the ETag argument within the `if-none-match` request header.
```

`./client.sh "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="`

```
2025-11-16 23:00:58 Status: 304

----------------------------------------
2025-11-16 23:00:59 Status: 304

----------------------------------------
2025-11-16 23:01:00 Status: 200 ETag: "MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg=="
{"etag":"\"MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg==\"","id":1,"name":"Keyboard","price":49.99,"updatedAt":"1 mins past the hour"}

----------------------------------------
2025-11-16 23:01:02 Status: 200 ETag: "MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg=="
{"etag":"\"MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg==\"","id":1,"name":"Keyboard","price":49.99,"updatedAt":"1 mins past the hour"}

----------------------------------------
```
