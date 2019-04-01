"use strict";

// Similar to the Obligation creation modal - see createObligationModal.js for comments.
angular.module('demoAppModule').controller('TransferModalCtrl', function ($http, $uibModalInstance, $uibModal, apiBaseURL, peers, id, refreshCallback) {
    const transferModal = this;

    transferModal.peers = peers;
    transferModal.id = id;
    transferModal.form = {};
    transferModal.formError = false;

    transferModal.transfer = () => {
        if (invalidFormInput()) {
            transferModal.formError = true;
        } else {
            transferModal.formError = false;

            const id = transferModal.id;
            const party = transferModal.form.counterparty;

            $uibModalInstance.close();

            const issueObligationEndpoint =
                apiBaseURL +
                `transfer-obligation?id=${id}&party=${party}`;

            $http.get(issueObligationEndpoint).then(
                (result) => { transferModal.displayMessage(result); refreshCallback(); },
                (result) => { transferModal.displayMessage(result); refreshCallback(); }

            );
        }
    };

    transferModal.displayMessage = (message) => {
        const transferMsgModal = $uibModal.open({
            templateUrl: 'transferMsgModal.html',
            controller: 'transferMsgModalCtrl',
            controllerAs: 'transferMsgModal',
            resolve: { message: () => message }
        });

        transferMsgModal.result.then(() => {}, () => {});
    };

    transferModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return transferModal.form.counterparty === undefined;
    }
});

angular.module('demoAppModule').controller('transferMsgModalCtrl', function ($uibModalInstance, message) {
    const transferMsgModal = this;
    transferMsgModal.message = message.data;
});