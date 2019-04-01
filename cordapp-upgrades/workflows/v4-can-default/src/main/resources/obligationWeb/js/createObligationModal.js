"use strict";

angular.module('demoAppModule').controller('CreateObligationModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers, refreshCallback) {
    const createObligationModal = this;

    createObligationModal.peers = peers;
    createObligationModal.form = {};
    createObligationModal.formError = false;

    /** Validate and create an Obligation. */
    createObligationModal.create = () => {
        if (invalidFormInput()) {
            createObligationModal.formError = true;
        } else {
            createObligationModal.formError = false;

            const amount = createObligationModal.form.amount;
            const currency = createObligationModal.form.currency;
            const party = createObligationModal.form.counterparty;

            $uibModalInstance.close();

            // We define the Obligation creation endpoint.
            const issueObligationEndpoint =
                apiBaseURL +
                `issue-obligation?amount=${amount}&currency=${currency}&party=${party}`;

            // We hit the endpoint to create the Obligation and handle success/failure responses.
            $http.get(issueObligationEndpoint).then(
                   (result) => { createObligationModal.displayMessage(result); refreshCallback();},
                   (result) => { createObligationModal.displayMessage(result); refreshCallback();}
            );
        }
    };

    /** Displays the success/failure response from attempting to create an Obligation. */
    createObligationModal.displayMessage = (message) => {
        const createObligationMsgModal = $uibModal.open({
            templateUrl: 'createObligationMsgModal.html',
            controller: 'createObligationMsgModalCtrl',
            controllerAs: 'createObligationMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createObligationMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the Obligation creation modal. */
    createObligationModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the Obligation.
    function invalidFormInput() {
        return isNaN(createObligationModal.form.amount) || (createObligationModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createObligationMsgModalCtrl', function($uibModalInstance, message) {
    const createObligationMsgModal = this;
    createObligationMsgModal.message = message.data;
});