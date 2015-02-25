'use strict';

kalipoApp.factory('Websocket', function (Thread) {

    return {

        unsubscribe: function (socket) {
            socket.unsubscribe();
        },

        subscribe: function (onMessage) {
            var socket = atmosphere;
            var subSocket;
            var transport = 'websocket';

            var request = {
                url: 'websocket/live/channel',
                contentType: "application/json",
                transport: transport,
                trackMessageLength: true,
                reconnectInterval: 5000,
                enableXDR: true,
                timeout: 60000
            };

            request.onOpen = function (response) {
                transport = response.transport;
                request.uuid = response.request.uuid;
            };

            request.onMessage = function (response) {
                var message = atmosphere.util.parseJSON(response.responseBody);
                onMessage(message);
            };

            socket.subscribe(request);

            return socket;
        }

    }
});