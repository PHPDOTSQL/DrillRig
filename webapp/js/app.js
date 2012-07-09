'use strict';

// Declare app level module which depends on filters, and services
var DrillRig = angular.module('DrillRig',
		[ 'DrillRig.config', 'DrillRig.monitor', 'DrillRig.navigation' ])


/**
 * dialog services 
 */
.service('dialogServices', [ '$q', function($q) {

	return {
		showDialog : function(dialogId) {
			$(dialogId).dialog("open");
		},

		createDialog : function(dialogId, options) {

			var dialog = $(dialogId);
			dialog.dialog(options);
		},

		destroyDialog : function(dialogId) {
			$(dialogId).dialog("destroy").remove();
		},

		closeDialog : function(dialogId) {
			$(dialogId).dialog("close");
		}
	}
} ]);