"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/option/";

    // Retrieves the identity of this, the other nodes, and the stocks the oracle can provide prices for.
    let peers = [];
    let stocks = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);
    $http.get(apiBaseURL + "stocks").then((response) => stocks = response.data.stocks);

    /** Displays the Option creation modal. */
    demoApp.openCreateOptionModal = () => {
        const createOptionModal = $uibModal.open({
            templateUrl: 'createOptionModal.html',
            controller: 'CreateOptionModalCtrl',
            controllerAs: 'createOptionModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                stocks: () => stocks
            }
        });
        // Ignores the modal result events.
        createOptionModal.result.then(() => {}, () => {});
    };

    /** Displays the cash issuance modal. */
    demoApp.openIssueCashModal = () => {
        const issueCashModal = $uibModal.open({
            templateUrl: 'issueCashModal.html',
            controller: 'IssueCashModalCtrl',
            controllerAs: 'issueCashModal',
            resolve: {
                apiBaseURL: () => apiBaseURL
            }
        });

        issueCashModal.result.then(() => {}, () => {});
    };

    /** Displays the Option trade modal. */
    demoApp.openTradeModal = (id) => {
        const tradeModal = $uibModal.open({
            templateUrl: 'tradeModal.html',
            controller: 'TradeModalCtrl',
            controllerAs: 'tradeModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                id: () => id
            }
        });

        tradeModal.result.then(() => {}, () => {});
    };

    /** Displays the Option exercise modal. */
    demoApp.openSettleModal = (id) => {
        const settleModal = $uibModal.open({
            templateUrl: 'settleModal.html',
            controller: 'SettleModalCtrl',
            controllerAs: 'settleModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                id: () => id
            }
        });

        settleModal.result.then(() => {}, () => {});
    };

    /** Refreshes the front-end. */
    demoApp.refresh = () => {
        // Update the list of Options.
        $http.get(apiBaseURL + "options").then((response) => demoApp.options =
            Object.keys(response.data).map((key) => response.data[key].state.data));

        // Update the cash balances.
        $http.get(apiBaseURL + "cash").then((response) => demoApp.cashBalances =
            response.data);
    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);