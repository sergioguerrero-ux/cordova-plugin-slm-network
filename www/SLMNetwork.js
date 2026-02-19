var exec = require('cordova/exec');

var SLMNetwork = {

    /**
     * Obtiene informacion de la conexion actual (one-shot).
     * Retorna: { type, isConnected, isExpensive, details }
     */
    getConnectionInfo: function (success, error) {
        exec(success, error, 'SLMNetwork', 'getConnectionInfo', []);
    },

    /**
     * Inicia monitoreo continuo de red (keepCallback).
     * @param {function} success - Callback que recibe actualizaciones
     */
    startMonitoring: function (success, error) {
        exec(success, error, 'SLMNetwork', 'startMonitoring', []);
    },

    /**
     * Detiene el monitoreo de red.
     */
    stopMonitoring: function (success, error) {
        exec(success, error, 'SLMNetwork', 'stopMonitoring', []);
    }
};

module.exports = SLMNetwork;
