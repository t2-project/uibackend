 UI BACKEND

This Service is the UI Backend. 
It wants to be an API Gateway and talks to a the Cart, Inventory and Orchestrator service.

## Build and Run

_**TODO: copy from Order was mangus checked it.**_

## HTTP Endpoints

The UI Backend has the following HTTP endpoints:

* ``/`` GET a greeting 
* ``/products/all`` GET list of all products in the inventory 
* ``/cart`` GET list of all products in cart of a current session
* ``/products/add`` POST here do add reservations to inventory and item to cart. returns a list of the products added.
* ``/products/delete`` POST here to delete an item from a cart (only cart, not the reservation) 
* ``/confirm`` POST saga request to orchestrator

## Usage

Assuming you have at least the [cart service](https://github.com/t2-project/cart) and the [inventory service](https://github.com/t2-project/inventory) (test profile is enough) up and running, and your UI Backend runs at ``http://localhost:8081`` you can interact with the UI Backend like this:

### Get all Products
request :
```
curl http://localhost:8081/products/all
```
response :
```
[
    {"id":"609a96a806573c12ed34479f","name":"Earl Grey (loose)","description":"very nice Earl Grey (loose) tea","units":529,"price":2.088258409676226},
[...]
    {"id":"609a96a906573c12ed3447ad","name":"Sencha (25 bags)","description":"very nice Sencha (25 bags) tea","units":101,"price":0.6923181656954707}
]
```

### Add products to your cart
You need the cookie for the HTTP Session and you want to replace ``<prodcutId>`` with the id of a product that is actually in your inventory.

request :
```
$ curl -c keks -i -X POST -H "Content-Type:application/json" -d '{"content": { "<productId>": 13}}' http://localhost:8081/products/add
```
response :
```
HTTP/1.1 200 
Set-Cookie: JSESSIONID=0A2371B08603E695F1730AC9F85F40BF; Path=/; HttpOnly
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 11 May 2021 14:45:36 GMT

[{"id":"609a96a906573c12ed3447a8","name":"Ceylon (20 bags)","description":"very nice Ceylon (20 bags) tea","units":475,"price":3.593564279221348}]
```

### Get the products in your cart
request :
```
$ curl -b keks http://localhost:8081/cart
```
response :
```
[{"id":"609a96a906573c12ed3447a8","name":"Ceylon (20 bags)","description":"very nice Ceylon (20 bags) tea","units":13,"price":3.593564279221348}]
```

### Delete a product from your cart
Once again, replace ``<prodcutId>`` with the id of a product that is actually in your inventory.
request : 
```
$ curl -b keks -i -X POST -H "Content-Type:application/json" -d '{"content": { "<productId>": 2}}' http://localhost:8081/products/delete
```
response :
```
HTTP/1.1 200 
Content-Length: 0
Date: Tue, 11 May 2021 14:51:00 GMT
```

You may get the cart once again, to see that the number of reserved units decreased.



### Confirm your order
This is possible, if you have the [orchestrator service](https://github.com/t2-project/orchestrator) up and running as well.

request : 
```
$ curl -b keks -i -X POST -H "Content-Type:application/json" -d '{"cardNumber":"num","cardOwner":"own","checksum":"checksum"}' http://localhost:8081/confirm
```
response :
```
TODO
```



## application properties 

property | read from env var | description |
-------- | ----------------- | ----------- |
t2.orchestrator.url | T2_ORCHESTRATOR_URL | url of the orchestrator service. inclusively endpoint and everything!
t2.cart.url | T2_CART_URL | url of the cart service 
t2.inventory.url | T2_INVENTORY_URL | url of the inventory service. 
t2.inventory.reservationendpoint | T2_RESERVATION_ENDPOINT | endpoint for reservations. sub path of the inventory url. guess it would be smarter to pass the entire url.
opentracing.jaeger.udp-sender.host | JAEGER_HOST | for the tracing. 

 