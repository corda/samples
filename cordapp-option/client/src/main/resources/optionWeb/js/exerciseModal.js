"use strict";

angular.module('demoAppModule').controller('SettleModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, id) {
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

            $uibModalInstance.close();

            const issueOptionEndpoint =
                apiBaseURL +
                `exercise-option?id=${id}`;

            $http.get(issueOptionEndpoint).then(
                (result) => settleModal.displayMessage(result),
                (result) => settleModal.displayMessage(result)
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
        return false;
    }
});

angular.module('demoAppModule').controller('settleMsgModalCtrl', function($uibModalInstance, message) {
    const settleMsgModal = this;
    settleMsgModal.message = message.data;
});