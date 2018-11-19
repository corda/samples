"use strict";

angular.module('demoAppModule').controller('CreateOptionModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers, stocks) {
    const createOptionModal = this;

    createOptionModal.peers = peers;
    createOptionModal.stocks = stocks;
    createOptionModal.optionTypes = ["CALL", "PUT"];
    createOptionModal.form = {};
    createOptionModal.formError = false;

    /** Validate and create an Option. */
    createOptionModal.create = () => {
        if (invalidFormInput()) {
            createOptionModal.formError = true;
        } else {
            createOptionModal.formError = false;

            const strike = createOptionModal.form.strike;
            const currency = createOptionModal.form.currency;
            const expiry = createOptionModal.form.expiry;
            const underlying = createOptionModal.form.underlying;
            const issuer = createOptionModal.form.issuer;
            const optionType = createOptionModal.form.optionType;

            $uibModalInstance.close();

            // We define the Option creation endpoint.
            const issueOptionEndpoint =
                apiBaseURL +
                `issue-option?strike=${strike}&currency=${currency}&expiry=${expiry}&underlying=${underlying}&issuer=${issuer}&optionType=${optionType}`;

            // We hit the endpoint to create the Option and handle success/failure responses.
            $http.get(issueOptionEndpoint).then(
                (result) => createOptionModal.displayMessage(result),
                (result) => createOptionModal.displayMessage(result)
            );F
        }
    };

    /** Displays the success/failure response from attempting to create an Option. */
    createOptionModal.displayMessage = (message) => {
        const createOptionMsgModal = $uibModal.open({
            templateUrl: 'createOptionMsgModal.html',
            controller: 'createOptionMsgModalCtrl',
            controllerAs: 'createOptionMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createOptionMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the Option creation modal. */
    createOptionModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the Option.
    function invalidFormInput() {
        return isNaN(createOptionModal.form.strike) || (createOptionModal.form.issuer === undefined) || (createOptionModal.form.underlying === undefined)
            || (createOptionModal.form.currency === undefined) || (createOptionModal.form.optionType === undefined);
        ;
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createOptionMsgModalCtrl', function($uibModalInstance, message) {
    const createOptionMsgModal = this;
    createOptionMsgModal.message = message.data;
});