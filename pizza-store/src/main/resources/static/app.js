const stompClient = new StompJs.Client({
});


let waiting = false;
var currentOrderId = "";
var currentOrderLastState = "";
var bag = new Map();
var bagObject = [];
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
        //console.log(JSON.parse(event.body));
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

async function placeOrderPrompt(){
    console.log("Placing Order with Prompt: " + $("textarea#prompt").val());
    console.log("With OrderId: " + $("input#orderId").val());
    currentOrderId = $("input#orderId").val();
    //Send Order to store
    const response = await fetch("/order", {
            method: "POST",
            body: JSON.stringify({
                              id: $("input#orderId").val(),
                              customer: {
                                  name: "salaboy",
                                  email: "salaboy@mail.com",
                              },
                              prompt: $("textarea#prompt").val()
                          }),
            headers: {
                "Content-type": "application/json; charset=UTF-8"
            }
        });
    const result = await response.json();
    console.log(result);
    currentOrderId = result.id;
    $("input#orderId").val(currentOrderId);
}

function emptyCart() {
  bagObject = [];
  bag = new Map();
  createBag();
}

async function placeOrder() {
    console.log("Placing Order");
    console.log("With OrderId: " + $("input#orderId").val());
    currentOrderId = $("input#orderId").val();
    //Send Order to store
    const response = await fetch("/order", {
        method: "POST",
        body: JSON.stringify({
            id: $("input#orderId").val(),
            customer: {
                name: "salaboy",
                email: "salaboy@mail.com",
            },
            items: bagObject
        }),
        headers: {
            "Content-type": "application/json; charset=UTF-8"
        }
    });

    const result = await response.json();
    console.log(result);
    currentOrderId = result.id;
    $("input#orderId").val(currentOrderId);
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function addVegetarianToBag(){
    if(!bag.has("vegetarian")){
      bag.set("vegetarian", 1);
    } else{
      bag.set("vegetarian", bag.get("vegetarian") + 1);
    }
    console.log("Bag Vegetarian:" + bag.get("vegetarian"));
    createBag();
}

function addDietCockToBag(){
    if(!bag.has("dietcoke")){
      bag.set("dietcoke", 1);
    } else{
      bag.set("dietcoke", bag.get("dietcoke") + 1);
    }
    console.log("Bag dietcoke:" + bag.get("dietcoke"));
    createBag();

}

function addPepperoniToBag(){
    if(!bag.has("pepperoni")){
          bag.set("pepperoni", 1);
        } else{
          bag.set("pepperoni", bag.get("pepperoni") + 1);
        }

        console.log("Bag pepperoni:" + bag.get("pepperoni"));
        createBag();
}

function createBag(){
    bagObject = [];
    if(bag.has("pepperoni")){
        bagObject.push({ name: "pepperoni", category: "pizza", amount: bag.get("pepperoni")});
    }
    if(bag.has("vegetarian")){
        bagObject.push({ name: "vegetarian", category: "pizza", amount: bag.get("vegetarian")});
    }
    if(bag.has("dietcoke")){
        bagObject.push({ name: "dietcoke", category: "drink", amount: bag.get("dietcoke")});
    }
    console.log(bagObject);
    $("#bag").empty();
    $("#bag").append("BAG: " + JSON.stringify(bagObject));
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



async function showEvent(event) {


    eventObject = JSON.parse(event);

    if (currentOrderId == eventObject.order.id && !waiting){
        console.log("Event Type => " + eventObject.type + " -> currentOrderLastState=> " + currentOrderLastState);

         if (eventObject.type === "ai-down") {
            $("#status").append(createItem("Error.png", "Something failed while processing your order.", false));

            $("#events").append(createEventEntry(eventObject));
            currentOrderLastState = "";
            currentOrderId = "";
            return;
         }

        if (eventObject.type === "order-processed-by-ai") {
            $("#status").append(createItem("Robot.png", "Doing AI stuff with your pizza order", false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

        if (eventObject.type === "order-placed") {
            $("#status").append(createItem("Order.png", "Order Placed" + JSON.stringify(eventObject.order.items), false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

        if (eventObject.type === "order-in-preparation" && currentOrderLastState === "order-placed"){
            $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            waiting = true;
            //console.log("Simulating waiting for kitchen to prepare the order");
            await new Promise(r => setTimeout(r, 1000));
            //console.log("Kitchen should have prepared the order by now");
            waiting = false;
            return;
        }

        if (eventObject.type === "order-in-preparation" && currentOrderLastState === "order-in-preparation"){
             currentOrderLastState = eventObject.type;
             $("#events").append(createEventEntry(eventObject));
             return;
        }

        if (eventObject.type === "order-ready" && currentOrderLastState === "order-in-preparation") {
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

        if (eventObject.type === "order-out-for-delivery" && currentOrderLastState === "order-ready" ) {
            $("#status").append(createItem("Map.gif", "Your order is out for delivery.", false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            waiting = true;
            //console.log("Simulating waiting for delivery to get to you");
            await new Promise(r => setTimeout(r, 1000));
            //console.log("Delivery should be almost there by now");
            waiting = false;
            return;
        }

        if (eventObject.type === "order-on-its-way" && currentOrderLastState === "order-out-for-delivery" ) {
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

         if (eventObject.type === "order-on-its-way" && currentOrderLastState === "order-on-its-way" ) {
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
         }

        if (eventObject.type === "order-completed" && currentOrderLastState === "order-on-its-way" ) {
            $("#status").append(createItem("BoxAndDrink.png", "Your order is now complete. Thanks for choosing us!", false));
            $("#events").append(createEventEntry(eventObject));
            currentOrderId = "";
            currentOrderLastState = "";
            currentOrderLastState = eventObject.type;
            return;
        }
    }else{
           console.log("Discarding event ("+eventObject.type+") for order: " + eventObject.order.id + " as current order is: " + currentOrderId);
    }

}



$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#placeOrder").click(() => placeOrder());
    $("#emptyCart").click(() => emptyCart());

    $("#placeOrderPrompt").click(() => placeOrderPrompt());
    $("#placeOrderFake").click(() => placeOrderFake());
    $("#kitchenAcceptFake").click(() => kitchenAcceptFake());
    $("#deliveryFake").click(() => deliveryFake());
    $("#deliveryUpdateFake").click(() => deliveryUpdateFake());
    $("#completedFake").click(() => completedFake());
    $("#disconnect").click(() => disconnect());
    $("#pepperoni-add").click(() => addPepperoniToBag());
    $("#vegetarian-add").click(() => addVegetarianToBag());
    $("#dietcoke-add").click(() => addDietCockToBag());

});