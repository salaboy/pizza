const stompClient = new StompJs.Client({
});


let waiting = false;
var currentOrderId = "";
var currentOrderLastState = "";
var bag = new Map();
var bagObject = [];
var bagEmojis = [];
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


window.addEventListener('load', function () {
  // Your document is loaded.
  var fetchInterval = 3000; // 5 seconds.

  // Invoke the request every 5 seconds.
  setInterval(fetchStatus, fetchInterval);
});

function fetchStatus() {
  fetch('/status')
    .then(function (response) {
      return response.json();
    }).then(function (data){
       console.log(data);
       updateServiceStatusIndicator(data);
    })
    .catch(function (err) {
      console.log('error: ' + err);
      updateServiceStatusIndicator({ error: 'Failed to fetch status' });
    });
}

function updateServiceStatusIndicator(statusData) {
  const indicator = document.getElementById('serviceStatusIndicator');
  
  if (statusData.error) {
    indicator.innerHTML = '<span class="status-error">❌ Error loading status</span>';
    return;
  }
  
  if (!statusData || Object.keys(statusData).length === 0) {
    indicator.innerHTML = '<span class="status-unknown">❓ Status unknown</span>';
    return;
  }
  
  let statusHtml = '';
  for (const [serviceName, serviceStatus] of Object.entries(statusData)) {
    const statusIcon = serviceStatus === true ? '✅' : '❌';
    const statusClass = serviceStatus === true ? 'status-up' : 'status-down';
    statusHtml += `<div class="service-status-item ${statusClass}">${serviceName}: ${statusIcon}</div>`;
  }
  
  indicator.innerHTML = statusHtml;
}


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
    var order = {
                                              customer: {
                                                  name: "salaboy",
                                                  email: "salaboy@mail.com",
                                              },
                                              prompt: $("textarea#prompt").val()
                                          }
    currentOrderId && (order.id = currentOrderId);

    // No order id has been provided, let's generate one
    if(!order.id){
            order.id = crypto.randomUUID();
            currentOrderId = order.id;

    }
    $("input#orderId").val(currentOrderId);
    //Send Order to store
    fetch("/order", {
            method: "POST",
            body: JSON.stringify(order),
            headers: {
                "Content-type": "application/json; charset=UTF-8"
            }
        });

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

    var order = {
                   customer: {
                      name: "salaboy",
                      email: "salaboy@mail.com",
                   },
                   items: bagObject
    };

    currentOrderId && (order.id = currentOrderId);

    // No order id has been provided, let's generate one
    if(!order.id){
        order.id = crypto.randomUUID();
        currentOrderId = order.id;
    }
    $("input#orderId").val(currentOrderId);

    //Send Order to store
    fetch("/order", {
        method: "POST",
        body: JSON.stringify(order),
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

function addVegetarianToBag(){
    if(!bag.has("vegetarian")){
      bag.set("vegetarian", 1);
    } else{
      bag.set("vegetarian", bag.get("vegetarian") + 1);
    }
    console.log("Bag Vegetarian:" + bag.get("vegetarian"));
    createBag();
}

function addDietCokeToBag(){
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

function addBeerToBag(){
    if(!bag.has("beer")){
          bag.set("beer", 1);
        } else{
          bag.set("beer", bag.get("beer") + 1);
        }

        console.log("Bag beer:" + bag.get("beer"));
        createBag();
}

function createBag(){
    bagObject = [];
    bagEmojis = [];
    if(bag.has("pepperoni")){
        bagObject.push({ name: "pepperoni", category: "pizza", amount: bag.get("pepperoni")});
        for(let i = 0; i < bag.get("pepperoni"); i ++){
          bagEmojis.push("🍕");
        }
    }
    if(bag.has("vegetarian")){
        bagObject.push({ name: "vegetarian", category: "pizza", amount: bag.get("vegetarian")});
        for(let i = 0; i < bag.get("vegetarian"); i ++){
          bagEmojis.push("🥒");
        }
    }
    if(bag.has("dietcoke")){
        bagObject.push({ name: "dietcoke", category: "drink", amount: bag.get("dietcoke")});
        for(let i = 0; i < bag.get("dietcoke"); i ++){
           bagEmojis.push("🥤");
        }
    }
    if(bag.has("beer")){
        bagObject.push({ name: "beer", category: "drink", amount: bag.get("beer")});
        for(let i = 0; i < bag.get("beer"); i ++){
           bagEmojis.push("🍺");
        }
    }
    console.log(bagObject);

    $("#bag").empty();
    var bagItems = "";
    for(let i = 0; i < bagEmojis.length; i++){
        bagItems += "<div class='bag-item'>" +
            bagEmojis[i] +
            "</div>";
    }
    $("#bag").append(bagItems);
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
        "<div class='details'>";

    if(detailsImage !== ""){
      item += "<img src='imgs/" + detailsImage + "'/>";
    }

    item +=   "<p>" + text + "</p>" +
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

function createBagFromOrderItems(items){
    var emojisFromItems = [];
    for(let i = 0; i < items.length; i++){
        for(let j = 0; j < items[i].amount; j++){
            if(items[i].name === "pepperoni"){
               emojisFromItems.push("🍕");
            }else if(items[i].name === "vegetarian"){
               emojisFromItems.push("🥒");
            }else if(items[i].name === "dietcoke"){
               emojisFromItems.push("🥤");
            }else if(items[i].name === "beer"){
               emojisFromItems.push("🍺");
            }

        }
    }
    var bagItems = "<div class='bag' style='flex-wrap: wrap;justify-content: left;'><div class='bag-items' style='flex-wrap: wrap; justify-content: left;'>";
    for(let i = 0; i < emojisFromItems.length; i++){
        bagItems += "<div class='bag-item'>" +
            emojisFromItems[i] +
            "</div>";
    }
    bagItems += "</div></div>";

    return bagItems;
}


async function showEvent(event) {


    eventObject = JSON.parse(event);
    console.log("Event Type => " + eventObject.type + " -> currentOrderLastState=> " + currentOrderLastState);
    console.log("Waiting => " + !waiting);
    if (currentOrderId == eventObject.order.id && !waiting){

         if (eventObject.type === "ai-down") {
            $("#status").append(createItem("Broken-Robot.png", "Something failed while processing your order.", false));

            $("#events").append(createEventEntry(eventObject));
            currentOrderLastState = "";
            currentOrderId = "";
            return;
         }

         if (eventObject.type === "storing-issue") {
            $("#status").append(createItem("Broken-Robot.png", "Something failed while processing your order.", false));
            $("#events").append(createEventEntry(eventObject));
            currentOrderLastState = "";
            currentOrderId = "";
            return;
         }

         if (eventObject.type === "kitchen-issue") {
            $("#status").append(createItem("Broken-Robot.png", "Something failed while processing your order.", false));
            $("#events").append(createEventEntry(eventObject));
            currentOrderLastState = "";
            currentOrderId = "";
            return;
         }

         if (eventObject.type === "delivery-issue") {
            $("#status").append(createItem("Broken-Robot.png", "Something failed while processing your order.", false));
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

            $("#status").append(createItem("", createBagFromOrderItems(eventObject.order.items) + "<br/><h3>Order Placed</h3>" , false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

        if (eventObject.type === "order-in-preparation" && currentOrderLastState === "order-placed"){
            $("#status").append(createItem("PizzaInOven.png", "Your Order is being prepared.", false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            if(currentOrderId === "789-456-123" || currentOrderId === "abc-def-ghi"){
//                waiting = true;
//                //console.log("Simulating waiting for kitchen to prepare the order");
//                await new Promise(r => setTimeout(r, 1000));
//                //console.log("Kitchen should have prepared the order by now");
//                waiting = false;
            }
            return;
        }

        if (eventObject.type === "order-in-preparation" && currentOrderLastState === "order-in-preparation"){
             currentOrderLastState = eventObject.type;
             $("#events").append(createEventEntry(eventObject));
             return;
        }

        if (eventObject.type === "order-ready" && currentOrderLastState === "order-in-preparation")  {
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            return;
        }

        if (eventObject.type === "order-out-for-delivery" && ( currentOrderLastState === "order-ready"  || currentOrderLastState === "order-placed" )) {
            $("#status").append(createItem("Map.gif", "Your order is out for delivery.", false));
            currentOrderLastState = eventObject.type;
            $("#events").append(createEventEntry(eventObject));
            if(currentOrderId === "789-456-123" || currentOrderId === "abc-def-ghi"){
//                waiting = true;
//                //console.log("Simulating waiting for delivery to get to you");
//                await new Promise(r => setTimeout(r, 1000));
//                //console.log("Delivery should be almost there by now");
//                waiting = false;
            }
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
           console.log("Discarding event ("+eventObject.type+") for order: " + eventObject.order.id + " as current order is: " + currentOrderId + " -> !waiting? " + !waiting);
    }

}

function setTab1() {
    $("#tab1").addClass("active"); 
    $("#tab2").removeClass("active");
    $("#tab2-content").removeClass("active");
    $("#tab1-content").addClass("active");    
}

function setTab2() {
    $("#tab2").addClass("active"); 
    $("#tab1").removeClass("active");
    $("#tab1-content").removeClass("active");
    $("#tab2-content").addClass("active");
}

function setManualId() {
    if ($("#manualID").is(":checked")) {
        $("input#orderId").prop("disabled", false);
        $("input#orderId").val("789-456-123")
    } else {
        $("input#orderId").prop("disabled", true);
        $("input#orderId").val("");
        currentOrderId = "";
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
    $("#dietcoke-add").click(() => addDietCokeToBag());
    $("#beer-add").click(() => addBeerToBag());

    $("#tab1").click(() => setTab1());
    $("#tab2").click(() => setTab2());

    $("#manualID").click(() => setManualId());

});