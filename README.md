# UI Backend Service

This service is the UI Backend.
It wants to be an API Gateway and talks to the Cart, Inventory and Orchestrator service.

## Build and Run

Refer to the [Documentation](https://t2-documentation.readthedocs.io/en/latest/microservices/deploy.html) on how to build, run or deploy the T2-Project services.

## HTTP Endpoints

The UI Backend has the following HTTP endpoints:

* `/products` GET list of all products in the inventory
* `/cart` GET list of all products in cart of a specific session
* `/cart` POST list of products to add/update/delete in/from cart of a specific session
* `/confirm` POST saga request to orchestrator

## Usage

Assuming you have at least the [cart service](https://github.com/t2-project/cart) and the [inventory service](https://github.com/t2-project/inventory) (test profile is enough) up and running, and your UI Backend runs at `http://localhost:8081` you can interact with the UI Backend like this:

### Get all products

```sh
curl http://localhost:8081/products
```

```json5
[
    {"id":"609a96a806573c12ed34479f","name":"Earl Grey (loose)","description":"very nice Earl Grey (loose) tea","units":529,"price":2.088258409676226},
// [...]
    {"id":"609a96a906573c12ed3447ad","name":"Sencha (25 bags)","description":"very nice Sencha (25 bags) tea","units":101,"price":0.6923181656954707}
]
```

### Add products to your cart

You want to replace `{sessionId}` with an id for your session and `<prodcutId>` with the id of a product that is actually in your inventory.

```sh
curl -i -X POST -H "Content-Type:application/json" -d '{"content": { "<productId>": 13}}' http://localhost:8081/cart/{sessionId}
```

```json
[
    {
        "id":"foo",
        "name":"Ceylon (20 bags)",
        "description":"very nice Ceylon (20 bags) tea",
        "units":13,
        "price":3.593564279221348
    }
]
```

### Get the products in your cart

```sh
curl http://localhost:8081/cart
```

```json
[{"id":"609a96a906573c12ed3447a8","name":"Ceylon (20 bags)","description":"very nice Ceylon (20 bags) tea","units":13,"price":3.593564279221348}]
```

### Delete a product from your cart

Once again, replace `{sessionId}` with an id for your session and `<prodcutId>` with the id of a product that is actually in your inventory.

```sh
curl -i -X POST -H "Content-Type:application/json" -d '{"content": { "<productId>": 2}}' http://localhost:8081/cart/{sessionId}
```

You may GET the cart once again, to see that the number of reserved units decreased.

### Confirm your order

This is possible, if you have the [orchestrator service](https://github.com/t2-project/orchestrator) up and running as well.

```sh
curl -i -X POST -H "Content-Type:application/json" -d '{"cardNumber":"num","cardOwner":"own","checksum":"sum", "sessionId":"<sessionId>"}' http://localhost:8081/confirm
```

## Application Properties

| property                           | read from env var                | description                                                                                                                                                   |
|------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| t2.orchestrator.url                | T2_ORCHESTRATOR_URL              | url of the orchestrator service. inclusively endpoint and everything!                                                                                         |
| t2.cart.url                        | T2_CART_URL                      | url of the cart service                                                                                                                                       |
| t2.inventory.url                   | T2_INVENTORY_URL                 | url of the inventory service.                                                                                                                                 |
| t2.inventory.reservationendpoint   | T2_RESERVATION_ENDPOINT          | endpoint for reservations. sub path of the inventory url. guess it would be smarter to pass the entire url.                                                   |
| t2.computation-simulator.enabled   | T2_COMPUTATION_SIMULATOR_ENABLED | boolean value, defaults to false. if true, the service computation-simulator gets called when an order is confirmed to simulate a compute intensive scenario. |
| t2.computation-simulator.url       | T2_COMPUTATION_SIMULATOR_URL     | url of the computation-simulator service.                                                                                                                     |
| opentracing.jaeger.udp-sender.host | T2_JAEGER_HOST                   | for the tracing.                                                                                                                                              |
