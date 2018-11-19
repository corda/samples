"use strict";

angular.module('demoAppModule').controller('TradeModalCtrl', function ($http, $uibModalInstance, $uibModal, apiBaseURL, peers, id) {
    const tradeModal = this;

    tradeModal.peers = peers;
    tradeModal.id = id;
    tradeModal.form = {};
    tradeModal.formError = false;

    tradeModal.trade = () => {
        if (invalidFormInput()) {
            tradeModal.formError = true;
        } else {
            tradeModal.formError = false;

            const id = tradeModal.id;
            const newOwner = tradeModal.form.newOwner;

            $uibModalInstance.close();

            const issueOptionEndpoint =
                apiBaseURL +
                `trade-option?id=${id}&newOwner=${newOwner}`;

            $http.get(issueOptionEndpoint).then(
                (result) => tradeModal.displayMessage(result),
                (result) => tradeModal.displayMessage(result)
            );
        }
    };

    tradeModal.displayMessage = (message) => {
        const tradeMsgModal = $uibModal.open({
            templateUrl: 'tradeMsgModal.html',
            controller: 'tradeMsgModalCtrl',
            controllerAs: 'tradeMsgModal',
            resolve: { message: () => message }
        });

        tradeMsgModal.result.then(() => {}, () => {});
    };

    tradeModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return tradeModal.form.newOwner === undefined;
    }
});

angular.module('demoAppModule').controller('tradeMsgModalCtrl', function ($uibModalInstance, message) {
    const tradeMsgModal = this;
    tradeMsgModal.message = message.data;
});