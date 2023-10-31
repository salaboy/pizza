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

function placeOrder(){
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
            items:[
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


function showEvent(event) {
    
    eventObject = JSON.parse(event);
    console.log("Event Type => "+ eventObject.type);
    $("#events").append("<tr><td>" + event + "</td></tr>");
    if(eventObject.type === "order-placed"){
        $( "#stepImg" ).attr("src","imgs/Order.png");
    }
    if(eventObject.type === "order-in-preparation"){
        $( "#stepImg" ).attr("src","imgs/GreenDot.png");
    }
    if(eventObject.type === "order-out-for-delivery"){
        $( "#stepImg" ).attr("src","imgs/Map.gif");
    }

}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#placeOrder" ).click(() => placeOrder());
    $( "#disconnect" ).click(() => disconnect());
});