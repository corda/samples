"use strict";

// Similar to the Obligation creation modal - see createObligationModal.js for comments.
angular.module('demoAppModule').controller('SettleModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, id, refreshCallback) {
    const settleModal = this;

    settleModal.id = id;
    settleModal.form = {};
    settleModal.formError = false;

    settleModal.settle = () => {
        if (invalidFormInput()) {
            settleModal.formError = true;
        } else {
            settleModal.formError = false;

            const id = settleModal.id;
            const amount = settleModal.form.amount;
            const currency = settleModal.form.currency;

            $uibModalInstance.close();

            const issueObligationEndpoint =
                apiBaseURL +
                `settle-obligation?id=${id}&amount=${amount}&currency=${currency}`;

            $http.get(issueObligationEndpoint).then(
                (result) => { settleModal.displayMessage(result); refreshCallback(); },
                (result) => { settleModal.displayMessage(result); refreshCallback(); }
            );
        }
    };

    settleModal.displayMessage = (message) => {
        const settleMsgModal = $uibModal.open({
            templateUrl: 'settleMsgModal.html',
            controller: 'settleMsgModalCtrl',
            controllerAs: 'settleMsgModal',
            resolve: {
                message: () => message
            }
        });

        settleMsgModal.result.then(() => {}, () => {});
    };

    settleModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return isNaN(settleModal.form.amount) || (settleModal.form.currency.length != 3);
    }
});

angular.module('demoAppModule').controller('settleMsgModalCtrl', function($uibModalInstance, message) {
    const settleMsgModal = this;
    settleMsgModal.message = message.data;
});