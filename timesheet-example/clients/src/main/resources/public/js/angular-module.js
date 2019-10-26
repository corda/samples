"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.getMyInvoices = () => $http.get(apiBaseURL + "my-invoices")
        .then((response) => demoApp.myinvoices = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getMyInvoices();

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.openPayInvoice = () => {
        const payInvoiceInstance = $uibModal.open({
            templateUrl: 'demoAppPayInvoice.html',
            controller: 'PayInvoiceCtrl',
            controllerAs: 'payInvoiceInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                invoices: () => demoApp.myinvoices.filter((invoice) => !invoice.paid).map((invoice) => invoice.linearId.id)
            }
        });

        payInvoiceInstance.result.then(() => {}, () => {});
    };
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

        // Validates and sends Invoice.
        modalInstance.create = function validateAndSendInvoice() {
            if (modalInstance.form.value <= 0) {
                modalInstance.formError = true;
            } else {
                modalInstance.formError = false;
                $uibModalInstance.close();

                let CREATE_INVOICES_PATH = apiBaseURL + "create-invoice"

                let createInvoiceData = $.param({
                    megacorp: modalInstance.form.megacorp,
                    hoursWorked : modalInstance.form.hoursWorked,
                    date: modalInstance.form.date
                });

                let createInvoiceHeaders = {
                    headers : {
                        "Content-Type": "application/x-www-form-urlencoded"
                    }
                };

                // Create Invoice  and handles success / fail responses.
                $http.post(CREATE_INVOICES_PATH, createInvoiceData, createInvoiceHeaders).then(
                    modalInstance.displayMessage,
                    modalInstance.displayMessage
                );
            }
        };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Invoice modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Invoice.
    function invalidFormInput() {
        return isNaN(modalInstance.form.value) || (modalInstance.form.counterparty === undefined);
    }
});

app.controller('PayInvoiceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, invoices) {
    const modalInstance = this;

    modalInstance.invoices = invoices;
    modalInstance.form = {};
    modalInstance.formError = false;

        // Validates and sends Invoice.
        modalInstance.create = function validateAndPayInvoice() {
            if (!modalInstance.form.invoiceId) {
                modalInstance.formError = true;
            } else {
                modalInstance.formError = false;
                $uibModalInstance.close();

                let PAY_INVOICES_PATH = apiBaseURL + "pay-invoice"

                let payInvoiceData = $.param({
                    invoiceId: modalInstance.form.invoiceId
                });

                let payInvoiceHeaders = {
                    headers : {
                        "Content-Type": "application/x-www-form-urlencoded"
                    }
                };

                // Create Invoice  and handles success / fail responses.
                $http.post(PAY_INVOICES_PATH, payInvoiceData, payInvoiceHeaders).then(
                    modalInstance.displayMessage,
                    modalInstance.displayMessage
                );
            }
        };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Invoice modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Invoice.
    function invalidFormInput() {
        return isNaN(modalInstance.form.value) || (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});