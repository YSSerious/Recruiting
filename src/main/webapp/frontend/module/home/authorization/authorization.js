/**
 * Created by dima on 30.04.16.
 */
'use strict';

angular.module('appAuthorization', [])
    .factory('TokenStorage', function () {
        var storageToken = 'auth_token';
        return {
            store: function (token) {
                $rootScope.authenticated = true;
                return localStorage.setItem(storageToken, token);
            },
            retrieve: function () {
                return localStorage.getItem(storageToken);
            },
            clear: function () {
                $rootScope.authenticated = false;
                return localStorage.removeItem(storageToken);
            }
        };
    }).factory('TokenAuthInterceptor', function ($q, TokenStorage) {
    return {
        request: function (config) {
            var authToken = TokenStorage.retrieve();
            if (authToken) {
                config.headers['X-AUTH-TOKEN'] = authToken;
            }
            return config;
        },
        responseError: function (error) {
            if (error.status === 401 || error.status === 403) {
                TokenStorage.clear();

            }
            return $q.reject(error);
        }
    };
});