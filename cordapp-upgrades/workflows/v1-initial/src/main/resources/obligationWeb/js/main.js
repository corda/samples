"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/obligation/";

    // Retrieves the identity of this and other nodes.
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    /** Displays the obligation creation modal. */
    demoApp.openCreateObligationModal = () => {
        const createObligationModal = $uibModal.open({
            templateUrl: 'createObligationModal.html',
            controller: 'CreateObligationModalCtrl',
            controllerAs: 'createObligationModal',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                refreshCallback: () => demoApp.refresh
            }
        });

        // Ignores the modal result events.
        createObligationModal.result.then(() => {}, () => {});
    };

    /** Displays the cash issuance modal. */
    demoApp.openIssueCashModal = () => {
        const issueCashModal = $uibModal.open({
            templateUrl: 'issueCashModal.html',
            controller: 'IssueCashModalCtrl',
            controllerAs: 'issueCashModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                refreshCallback: () => demoApp.refresh
            }
        });

        issueCashModal.result.then(() => {}, () => {});
    };

    /** Displays the Obligation transfer modal. */
    demoApp.openTransferModal = (id) => {
        const transferModal = $uibModal.open({
            templateUrl: 'transferModal.html',
            controller: 'TransferModalCtrl',
            controllerAs: 'transferModal',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                id: () => id,
                refreshCallback: () => demoApp.refresh
            }
        });

        transferModal.result.then(() => {}, () => {});
    };

    /** Displays the Obligation settlement modal. */
    demoApp.openSettleModal = (id) => {
        const settleModal = $uibModal.open({
            templateUrl: 'settleModal.html',
            controller: 'SettleModalCtrl',
            controllerAs: 'settleModal',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                id: () => id,
                refreshCallback: () => demoApp.refresh
            }
        });

        settleModal.result.then(() => {}, () => {});
    };

    /** Refreshes the front-end. */
    demoApp.refresh = () => {
        // Update the list of Obligations.
        $http.get(apiBaseURL + "obligations").then((response) => demoApp.obligations =
            Object.keys(response.data).map((key) => response.data[key]));

        // Update the cash balances.
        $http.get(apiBaseURL + "cash-balances").then((response) => demoApp.cashBalances =
            response.data);
    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);