# UI BACKEND

the ui backend. 
talk to this guy 'cause there is not yet a real ui.

## endpoints
* ``/`` GET a greeting 
* ``/products/all`` GET list of all products in the inventory 
* ``/cart`` GET list of all products in cart of a current session
* ``/products/add`` POST here do add reservations to inventory and item to cart. returns a list of the products added.
* ``/products/delete`` POST here to delete an item from a cart (only cart, not the reservation) 
* ``/confirm`` POST saga request to orchestrator

## application properties 

property | read from env var | description |
-------- | ----------------- | ----------- |
t2.orchestrator.url | T2_ORCHESTRATOR_URL | url of the orchestrator service. inclusively endpoint and everything!
t2.cart.url | T2_CART_URL | url of the cart service 
t2.inventory.url | T2_INVENTORY_URL | url of the inventory service. 
t2.inventory.reservationendpoint | T2_RESERVATION_ENDPOINT | endpoint for reservations. sub path of the inventory url. guess it would be smarter to pass the entire url.
opentracing.jaeger.udp-sender.host | JAEGER_HOST | for the tracing. 

 