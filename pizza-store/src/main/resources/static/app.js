const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

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

function connect() {
    stompClient.activate();
}

function placeOrder() {
    console.log("Placing Order");
    //Connect to websocket
    connect();
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
    })
        .then((response) => {
            response.json();
        }
        )
        .then((json) => console.log(json));



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

function showEvent(event) {

    eventObject = JSON.parse(event);
    console.log("Event Type => " + eventObject.type);

    $("#events").append("<tr><td>" + event + "</td></tr>");


    if (eventObject.type === "order-placed") {
        $("#status").empty();
        $("#status").append(createItem("Order.png", "Order Placed", false));
    }
    if (eventObject.type === "order-in-preparation") {
        $("#status").empty();
        $("#status").append(createItem("Order.png", "Order Placed", true));
        $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", false));
    }
    if (eventObject.type === "order-out-for-delivery") {
        $("#status").empty();
        $("#status").append(createItem("Order.png", "Order Placed", true));
        $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", true));
        $("#status").append(createItem("Map.gif", "Your order is out for delivery.", false));
    }
    if (eventObject.type === "order-completed") {
        $("#status").empty();
        $("#status").append(createItem("Order.png", "Order Placed", true));
        $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", true));
        $("#status").append(createItem("Map.gif", "Your order is out for delivery.", true));
        $("#status").append(createItem("BoxAndDrink.png", "Your order is now complete. Thanks for choosing us!", false));

    }

}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#placeOrder").click(() => placeOrder());
    $("#disconnect").click(() => disconnect());
});