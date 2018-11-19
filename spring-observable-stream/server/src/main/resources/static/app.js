"use strict";

const REST_BASE_PATH = "/yo";
const GET_ME_PATH = `${REST_BASE_PATH}/myname`;
const GET_PEERS_PATH = `${REST_BASE_PATH}/peersnames`;
const GET_YOS_PATH = `${REST_BASE_PATH}/getyos`;
const SEND_YO_PATH = `${REST_BASE_PATH}/sendyo`;
const STOMP_SUBSCRIBE_PATH = "/stomp";
const STOMP_RESPONSE_PATH = "/stompresponse";

const app = angular.module("yoAppModule", ["ui.bootstrap"]);

app.controller("YoAppController", function($scope, $http, $location, $uibModal) {
    const yoApp = this;
    let peers = [];

    // Retrieves my identity.
    (function retrieveMe() {
        $http.get(GET_ME_PATH)
            .then(function storeMe(response) {
                yoApp.me = response.data;
            })
    })();

    // Retrieves a list of network peers.
    (function retrievePeers() {
        $http.get(GET_PEERS_PATH)
            .then(function storePeers(response) {
                peers = response.data.peers;
            })
    })();

    // Starts streaming new Yo's from the websocket.
    (function connectAndStartStreamingYos() {
        let socket = new SockJS(STOMP_SUBSCRIBE_PATH);
        let stompClient = Stomp.over(socket);
        stompClient.connect({}, function startStreamingYos(frame) {
            stompClient.subscribe(STOMP_RESPONSE_PATH, function updateYos(update) {
                let yoState = JSON.parse(update.body);
                yoApp.yos.push(yoState);
                // Forces the view to refresh, showing the new Yo.
                $scope.$apply();
            });
        });
    })();

    // Opens the send-Yo modal.
    yoApp.openSendYoModal = function openSendYoModal() {
        $uibModal.open({
            templateUrl: "yoAppModal.html",
            controller: "SendYoModalController",
            controllerAs: "sendYoModal",
            resolve: {
                peers: () => peers
            }
        });
    };

    // Gets a list of existing Yo's.
    function getYos() {
        $http.get(GET_YOS_PATH).then(function processYos(response) {
            let yos = Object.keys(response.data)
                .map((key) => response.data[key]);
            yoApp.yos = yos;
        });
    }

    // Pre-populate the list of Yo's.
    getYos();
});

// Controller for send-yo modal.
app.controller("SendYoModalController", function ($http, $location, $uibModalInstance, $uibModal, peers) {
    const modalInstance = this;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validates and sends Yo.
    modalInstance.create = function validateAndSendYo() {
        if (isFormInvalid()) {
            modalInstance.formError = true;

        } else {
            modalInstance.formError = false;
            $uibModalInstance.close();

            let sendYoData = $.param({
                target: modalInstance.form.target
            });
            let sendYoHeaders = {
                headers : {
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            };

            // Sends Yo and handles success / fail responses.
            $http.post(SEND_YO_PATH, sendYoData, sendYoHeaders).then(
                modalInstance.displayMessage,
                modalInstance.displayMessage
            );
        }
    };

    // Display result message from sending Yo.
    modalInstance.displayMessage = function displayMessage(message) {
        $uibModal.open({
            templateUrl: "messageContent.html",
            controller: "ShowMessageController",
            controllerAs: "showMessageModal",
            resolve: { message: () => message }
        });
    };

    // Closes the send-Yo modal.
    modalInstance.cancel = $uibModalInstance.dismiss;

    // Validates the Yo before sending.
    function isFormInvalid() {
        return modalInstance.form.target === undefined;
    }
});

// Controller for success/fail modal dialogue.
app.controller('ShowMessageController', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});

// Intercepts unhandled-rejection errors.
app.config(["$qProvider", function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);