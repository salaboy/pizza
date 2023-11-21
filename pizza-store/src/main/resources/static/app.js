const stompClient = new StompJs.Client({
});

function connect() {
    console.log("Fetching Server Info")
    fetch("/server-info", {
        method: "GET",
        headers: {
            "Content-type": "application/json; charset=UTF-8"
        }
    }).then((response) => {
        console.log("Fetching Response")
        return response.json();
    }).then((response) => {
        var publicURL = 'ws://' + response.publicIp + '/ws';
        stompClient.brokerURL = publicURL;
        console.log(publicURL);
        console.log("Activating client")
        stompClient.activate();
    }).catch((error) => {
        console.error(`Could not get server-info: ${error}`);
    });

};

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/events', (event) => {
        console.log(JSON.parse(event.body));
        showEvent(event.body);

    });
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#events").html("");
}


function placeOrderFake() {
    var fakeEvent = {
        "type": "order-placed",
        "service": "store",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Order has been placed."
    }
    showEvent(JSON.stringify(fakeEvent));
}

function kitchenAcceptFake() {
    var fakeEvent = {
        "type": "order-in-preparation",
        "service": "kitchen",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Your Order has been accepted by the kitchen."
    }
    showEvent(JSON.stringify(fakeEvent));
}

function deliveryFake() {
    var fakeEvent = {
        "type": "order-out-for-delivery",
        "service": "kitchen",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Your Order is out for delivery."
    }
    showEvent(JSON.stringify(fakeEvent));
}

function deliveryUpdateFake() {
    var fakeEvent = {
        "type": "order-on-its-way",
        "service": "delivery",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Your Order 1 mile away"
    }
    showEvent(JSON.stringify(fakeEvent));
    var fakeEvent = {
        "type": "delivery",
        "service": "kitchen",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Your Order half mile away"
    }
    showEvent(JSON.stringify(fakeEvent));
}

function completedFake() {
    var fakeEvent = {
        "type": "order-completed",
        "service": "store",
        "order": {
            "id": "123-123-123-123-123-123"
        },
        "message": "Your has been delivered."
    }
    showEvent(JSON.stringify(fakeEvent));

}

function placeOrder() {
    console.log("Placing Order");

    //Send Order to store
    fetch("/order", {
        method: "POST",
        body: JSON.stringify({
            customer: {
                name: "salaboy",
                email: "salaboy@mail.com",
            },
            items: [
                {
                    "type": "pepperoni",
                    "amount": 1,
                }
            ]
        }),
        headers: {
            "Content-type": "application/json; charset=UTF-8"
        }
    });

}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function createItem(detailsImage, text, disabled) {
    var item = "<div class='item animate'>" +
        "<div class='green-dot'>";
    if (disabled) {
        item += "<img class='disabled transition' src='imgs/GreenDot.png'/>";
    } else {
        item += "<img class='transition' src='imgs/GreenDot.png'/>";
    }
    item += "</div>" +
        "<div class='details'>" +
        "<img src='imgs/" + detailsImage + "'/>" +
        "<p>" + text + "</p>" +
        "</div>" +
        "</div>";
    return item;
}

function createEventEntry(eventObject) {
    var eventEntry = "<div>" +
        "<p>Event from Service: <strong>" + eventObject.service + "</strong></p>" +
        "<p>Event Type: <strong>" + eventObject.type + "</strong></p>" +
        "<p>Message: <strong>" + eventObject.message + "</strong></p>" +
        "<p>Event Order Id: <strong>" + eventObject.order.id + "</strong></p>" +
        "</div>";
    return eventEntry;

}

function showEvent(event) {

    eventObject = JSON.parse(event);
    console.log("Event Type => " + eventObject.type);

    $("#events").append(createEventEntry(eventObject));


    if (eventObject.type === "order-placed") {
        $("#status").append(createItem("Order.png", "Order Placed", false));
    }
    if (eventObject.type === "order-in-preparation") {
        $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", false));
    }
    if (eventObject.type === "order-out-for-delivery") {

        $("#status").append(createItem("Map.gif", "Your order is out for delivery.", false));
    }
    if (eventObject.type === "order-completed") {

        $("#status").append(createItem("BoxAndDrink.png", "Your order is now complete. Thanks for choosing us!", false));

    }

}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#placeOrder").click(() => placeOrder());
    $("#placeOrderFake").click(() => placeOrderFake());
    $("#kitchenAcceptFake").click(() => kitchenAcceptFake());
    $("#deliveryFake").click(() => deliveryFake());
    $("#deliveryUpdateFake").click(() => deliveryUpdateFake());
    $("#completedFake").click(() => completedFake());
    $("#disconnect").click(() => disconnect());
});